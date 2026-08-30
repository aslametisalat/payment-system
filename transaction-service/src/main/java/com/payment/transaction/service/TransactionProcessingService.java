package com.payment.transaction.service;

import com.payment.transaction.client.AcquirerClient;
import com.payment.transaction.client.IssuerClient;
import com.payment.transaction.client.MerchantClient;
import com.payment.transaction.client.NetworkClient;
import com.payment.transaction.dto.AcquirerRequest;
import com.payment.transaction.dto.AcquirerResponse;
import com.payment.transaction.dto.AuthorizationRequest;
import com.payment.transaction.dto.AuthorizationResponse;
import com.payment.transaction.dto.MerchantValidationResult;
import com.payment.transaction.dto.RoutingRequest;
import com.payment.transaction.dto.RoutingResponse;
import com.payment.transaction.dto.TransactionRequest;
import com.payment.transaction.dto.TransactionResponse;
import com.payment.transaction.dto.TransactionStepDto;
import com.payment.transaction.model.Transaction;
import com.payment.transaction.model.TransactionStep;
import com.payment.transaction.repository.TransactionRepository;
import com.payment.common.enums.ResponseCode;
import com.payment.common.enums.TransactionStatus;
import com.payment.common.event.TransactionCompletedEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessingService {

    private final TransactionRepository transactionRepository;
    private final MerchantClient merchantClient;
    private final AcquirerClient acquirerClient;
    private final NetworkClient networkClient;
    private final IssuerClient issuerClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final JmsTemplate jmsTemplate;

    // One queue per subscriber, all fed the same event, rather than one
    // shared queue or a topic: a JMS Queue is point-to-point, so three
    // consumers reading the same queue name would each get a THIRD of the
    // messages (competed for, not fanned out) - that's exactly what
    // happened here in practice before this was split apart. A topic would
    // fan out correctly, but a non-durable subscriber that happens to be
    // restarting when an event fires just loses it forever. Three named
    // queues gives every one of the three services every event, with each
    // queue durably holding messages for its own consumer if that consumer
    // is briefly down - the same trade-off a real system makes with
    // per-consumer queues or durable topic subscriptions.
    private static final List<String> TRANSACTION_COMPLETED_QUEUES = List.of(
            "transaction.completed.settlement",
            "transaction.completed.reporting",
            "transaction.completed.notification");

    // Only these two downstream hops are safe to retry automatically - see
    // the resilience4j.retry comment in application.yml for why
    // merchant-service and issuer-service are deliberately excluded.
    private static final Set<String> RETRYABLE_TARGETS = Set.of("network-service", "acquirer-service");

    /**
     * Walks a transaction through the same chain a real card payment does:
     * merchant's own limits, the acquiring bank's fraud screening, the card
     * network's routing, and finally the issuing bank's approve/decline
     * call - each one a real HTTP hop to another service, short-circuiting
     * to DECLINED at whichever stage says no.
     */
    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.info("Processing transaction for merchant: {}", request.getMerchantId());

        // Idempotency: a caller that retried this exact request (its own
        // Feign call timed out, a circuit breaker's retry fired, ...) sends
        // the same key again. If we've already recorded a result for it,
        // hand back that result instead of re-running the flow - a second
        // real authorization would mean a second hold on the cardholder's
        // funds for one purchase.
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent replay for key {} - returning transaction {} unchanged",
                        idempotencyKey, existing.get().getId());
                return toResponse(existing.get());
            }
        }

        // STAN/RRN (ISO 8583 fields 11 and 37): generated once, at the
        // terminal in the real flow, and carried unchanged through every
        // hop of the switch. If a caller didn't send them (e.g. a direct
        // API call rather than through pos-terminal-service), the switch
        // assigns its own so every trace still has real reference numbers.
        String stan = (request.getStan() != null && !request.getStan().isBlank())
                ? request.getStan() : generateStan();
        String rrn = (request.getRrn() != null && !request.getRrn().isBlank())
                ? request.getRrn() : generateRrn();

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setMerchantId(request.getMerchantId());
        transaction.setCardNumber(request.getCardNumber());
        transaction.setTerminalId(request.getTerminalId());
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setStan(stan);
        transaction.setRrn(rrn);

        try {
            transaction = transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException raceLoser) {
            // Narrow race: two retries with the same key both passed the
            // check above before either committed. The database's unique
            // constraint on idempotency_key is what actually closes this
            // window - whichever insert loses falls back to the winner's row.
            log.warn("Idempotency key {} was inserted concurrently - returning the winning transaction",
                    idempotencyKey);
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(this::toResponse)
                    .orElseThrow(() -> raceLoser);
        }

        try {
            // Step 1: Merchant Service - is this merchant active and within
            // its daily/monthly volume limits for this amount?
            MerchantValidationResult merchantResult = callStep(transaction, "Merchant Validation",
                    "merchant-service",
                    () -> merchantClient.validateForTransaction(request.getMerchantId(), request.getAmount()),
                    r -> String.format("MTI 0100 | STAN %s | Merchant %s | %s",
                            stan, request.getMerchantId(), r.getMessage()));
            if (!merchantResult.isValid()) {
                log.warn("Merchant validation failed: {}", merchantResult.getMessage());
                return decline(transaction, ResponseCode.INVALID_MERCHANT.getCode(),
                        merchantResult.getMessage(), "merchant-service");
            }

            // Step 2: Acquirer Service - the merchant's bank runs fraud and
            // velocity screening before it will forward the transaction.
            AcquirerResponse acquirerResponse = callStep(transaction, "Acquirer Processing", "acquirer-service",
                    () -> acquirerClient.processAcquiring(AcquirerRequest.builder()
                            .merchantId(request.getMerchantId())
                            .cardNumber(request.getCardNumber())
                            .amount(request.getAmount())
                            .terminalId(request.getTerminalId())
                            .build()),
                    r -> String.format("MTI 0100 | STAN %s | Acquirer %s | Fraud score %s/100 | %s",
                            stan, r.getAcquirerId(), r.getFraudScore(), r.getMessage()));
            transaction.setAcquirerId(acquirerResponse.getAcquirerId());
            if (!Boolean.TRUE.equals(acquirerResponse.getApproved())) {
                log.warn("Acquirer declined: {}", acquirerResponse.getMessage());
                return decline(transaction, ResponseCode.SECURITY_VIOLATION.getCode(),
                        acquirerResponse.getMessage(), "acquirer-service");
            }

            // Step 3: Network Service - identify the card's network from its
            // BIN and the issuer it should be routed to.
            RoutingResponse routingResponse = callStep(transaction, "Network Routing", "network-service",
                    () -> networkClient.route(RoutingRequest.builder()
                            .cardNumber(request.getCardNumber())
                            .amount(request.getAmount())
                            .build()),
                    r -> String.format("MTI 0100 | STAN %s | BIN %s routed via %s to %s | network fee $%s",
                            stan, binOf(request.getCardNumber()), r.getNetwork(), r.getIssuerId(),
                            r.getNetworkFee()));
            transaction.setNetworkId(
                    routingResponse.getNetwork() != null ? routingResponse.getNetwork().name() : null);
            transaction.setIssuerId(routingResponse.getIssuerId());

            // Step 4: Issuer Service - the cardholder's own bank makes the
            // final approve/decline call.
            AuthorizationRequest authRequest = new AuthorizationRequest();
            authRequest.setCardNumber(request.getCardNumber());
            authRequest.setCvv(request.getCvv());
            authRequest.setAmount(request.getAmount());
            authRequest.setCurrency(request.getCurrency());
            authRequest.setMerchantId(request.getMerchantId());

            AuthorizationResponse authResponse = callStep(transaction, "Issuer Authorization", "issuer-service",
                    () -> issuerClient.authorize(authRequest),
                    r -> String.format("MTI 0110 | STAN %s | RRN %s | Resp %s-%s%s",
                            stan, rrn, r.getResponseCode(), r.getMessage(),
                            r.isApproved() ? " | Auth Code " + r.getAuthorizationCode() : ""));

            // Update transaction with response
            if (authResponse.isApproved()) {
                transaction.setStatus(TransactionStatus.AUTHORIZED);
                transaction.setAuthorizationCode(authResponse.getAuthorizationCode());
                transaction.setAuthorizedAt(LocalDateTime.now());
                log.info("Transaction authorized: {}", transaction.getId());
            } else {
                transaction.setStatus(TransactionStatus.DECLINED);
                log.warn("Transaction declined: {}", authResponse.getMessage());
            }

            transaction.setResponseCode(authResponse.getResponseCode());
            transaction.setResponseMessage(authResponse.getMessage());

        } catch (Exception e) {
            log.error("Transaction failed: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            // Feign exceptions embed the failing URL and response body in
            // getMessage(), which routinely blows past response_message's
            // 255-char column limit - truncating here keeps the save from
            // throwing a second, uncaught exception on top of the first.
            transaction.setResponseMessage(truncate("System error: " + e.getMessage(), 255));
        }

        return finish(transaction);
    }

    private static String truncate(String message, int maxLength) {
        if (message == null || message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength);
    }

    /**
     * Runs one Feign call to another service - through a per-service circuit
     * breaker, and a retry too if that hop is idempotent-safe (see
     * RETRYABLE_TARGETS) - recording how it went as a TransactionStep either
     * way: SUCCESS with a switch-style message built from the real response
     * by detailFormatter (MTI, STAN/RRN, response code - the same fields a
     * real acquirer/network log would show), or FAILED with the exception
     * message (a tripped-open breaker included), before re-throwing so the
     * outer catch in processTransaction can still fail the whole
     * transaction. This is what makes it possible to see *which* hop broke,
     * not just that something did.
     */
    private <T> T callStep(Transaction transaction, String stepName, String target, Supplier<T> call,
                            Function<T, String> detailFormatter) {
        long start = System.currentTimeMillis();
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(target);
            Supplier<T> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, call);
            if (RETRYABLE_TARGETS.contains(target)) {
                Retry retry = retryRegistry.retry(target);
                decorated = Retry.decorateSupplier(retry, decorated);
            }
            T result = decorated.get();
            addStep(transaction, stepName, target, "SUCCESS", detailFormatter.apply(result), start);
            return result;
        } catch (RuntimeException e) {
            addStep(transaction, stepName, target, "FAILED",
                    String.format("MTI 0100 | STAN %s | %s did not respond: %s",
                            transaction.getStan(), target, e.getMessage()),
                    start);
            throw e;
        }
    }

    private static String binOf(String cardNumber) {
        return cardNumber != null && cardNumber.length() >= 6 ? cardNumber.substring(0, 6) : "??????";
    }

    private static String generateStan() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }

    private static String generateRrn() {
        return String.format("%012d", Math.abs(ThreadLocalRandom.current().nextLong()) % 1000000000000L);
    }

    private void addStep(Transaction transaction, String stepName, String target, String status,
                          String detail, long startMillis) {
        transaction.getSteps().add(TransactionStep.builder()
                .stepName(stepName)
                .target(target)
                .status(status)
                .detail(truncate(detail, 255))
                .durationMs(System.currentTimeMillis() - startMillis)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private TransactionResponse decline(Transaction transaction, String responseCode, String message,
                                         String declinedBy) {
        transaction.setStatus(TransactionStatus.DECLINED);
        transaction.setResponseCode(responseCode);
        transaction.setResponseMessage(message);
        addStep(transaction, "Declined", declinedBy, "DECLINED",
                String.format("MTI 0110 | STAN %s | Resp %s-%s", transaction.getStan(), responseCode, message),
                System.currentTimeMillis());
        return finish(transaction);
    }

    private TransactionResponse finish(Transaction transaction) {
        transaction = transactionRepository.save(transaction);
        publishCompletionEvent(transaction);
        return toResponse(transaction);
    }

    /**
     * Hands the transaction's outcome off to whichever services care about
     * it after the fact - settlement, reporting, notification - instead of
     * calling them synchronously on the authorization path the way
     * merchant/acquirer/network/issuer are called above. None of those
     * three need to be up, fast, or even running for authorize() to
     * respond; if the broker itself is unreachable, that's logged and
     * swallowed rather than failing a transaction that already succeeded
     * over a completely unrelated system.
     */
    private void publishCompletionEvent(Transaction transaction) {
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .merchantId(transaction.getMerchantId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .responseCode(transaction.getResponseCode())
                .responseMessage(transaction.getResponseMessage())
                .authorizationCode(transaction.getAuthorizationCode())
                .occurredAt(LocalDateTime.now())
                .build();
        for (String queue : TRANSACTION_COMPLETED_QUEUES) {
            try {
                jmsTemplate.convertAndSend(queue, event);
            } catch (Exception e) {
                log.warn("Could not publish completion event for transaction {} to {}: {}",
                        transaction.getId(), queue, e.getMessage());
            }
        }
    }
    
    @Transactional
    public TransactionResponse refundTransaction(String id) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));

        boolean refundable = transaction.getStatus() == TransactionStatus.AUTHORIZED
                || transaction.getStatus() == TransactionStatus.CAPTURED
                || transaction.getStatus() == TransactionStatus.SETTLED;

        if (!refundable) {
            throw new IllegalStateException(
                "Only authorized, captured or settled transactions can be refunded (current status: "
                    + transaction.getStatus() + ")");
        }

        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setResponseMessage("Refunded");
        transaction = transactionRepository.save(transaction);
        log.info("Transaction refunded: {}", transaction.getId());

        return toResponse(transaction);
    }

    public TransactionResponse getTransaction(String id) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
        return toResponse(transaction);
    }
    
    public List<TransactionResponse> getMerchantTransactions(String merchantId) {
        return transactionRepository.findByMerchantId(merchantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    private TransactionResponse toResponse(Transaction transaction) {
        List<TransactionStepDto> steps = transaction.getSteps().stream()
            .map(s -> TransactionStepDto.builder()
                .stepName(s.getStepName())
                .target(s.getTarget())
                .status(s.getStatus())
                .detail(s.getDetail())
                .durationMs(s.getDurationMs())
                .timestamp(s.getTimestamp())
                .build())
            .collect(Collectors.toList());

        return TransactionResponse.builder()
            .id(transaction.getId())
            .merchantId(transaction.getMerchantId())
            .type(transaction.getType())
            .status(transaction.getStatus())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .authorizationCode(transaction.getAuthorizationCode())
            .responseCode(transaction.getResponseCode())
            .responseMessage(transaction.getResponseMessage())
            .stan(transaction.getStan())
            .rrn(transaction.getRrn())
            .createdAt(transaction.getCreatedAt())
            .steps(steps)
            .build();
    }
}
