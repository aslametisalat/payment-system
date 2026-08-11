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
import com.payment.transaction.model.Transaction;
import com.payment.transaction.repository.TransactionRepository;
import com.payment.common.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
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

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setMerchantId(request.getMerchantId());
        transaction.setCardNumber(request.getCardNumber());
        transaction.setTerminalId(request.getTerminalId());
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING);

        transaction = transactionRepository.save(transaction);

        try {
            // Step 1: Merchant Service - is this merchant active and within
            // its daily/monthly volume limits for this amount?
            MerchantValidationResult merchantResult = merchantClient.validateForTransaction(
                    request.getMerchantId(), request.getAmount());
            if (!merchantResult.isValid()) {
                log.warn("Merchant validation failed: {}", merchantResult.getMessage());
                return decline(transaction, "03", merchantResult.getMessage());
            }

            // Step 2: Acquirer Service - the merchant's bank runs fraud and
            // velocity screening before it will forward the transaction.
            AcquirerResponse acquirerResponse = acquirerClient.processAcquiring(AcquirerRequest.builder()
                    .merchantId(request.getMerchantId())
                    .cardNumber(request.getCardNumber())
                    .amount(request.getAmount())
                    .terminalId(request.getTerminalId())
                    .build());
            transaction.setAcquirerId(acquirerResponse.getAcquirerId());
            if (!Boolean.TRUE.equals(acquirerResponse.getApproved())) {
                log.warn("Acquirer declined: {}", acquirerResponse.getMessage());
                return decline(transaction, "63", acquirerResponse.getMessage());
            }

            // Step 3: Network Service - identify the card's network from its
            // BIN and the issuer it should be routed to.
            RoutingResponse routingResponse = networkClient.route(RoutingRequest.builder()
                    .cardNumber(request.getCardNumber())
                    .amount(request.getAmount())
                    .build());
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

            AuthorizationResponse authResponse = issuerClient.authorize(authRequest);

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

    private TransactionResponse decline(Transaction transaction, String responseCode, String message) {
        transaction.setStatus(TransactionStatus.DECLINED);
        transaction.setResponseCode(responseCode);
        transaction.setResponseMessage(message);
        return finish(transaction);
    }

    private TransactionResponse finish(Transaction transaction) {
        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
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
            .createdAt(transaction.getCreatedAt())
            .build();
    }
}
