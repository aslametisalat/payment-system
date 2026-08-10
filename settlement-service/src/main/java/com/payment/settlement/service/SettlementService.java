package com.payment.settlement.service;

import com.payment.common.enums.SettlementStatus;
import com.payment.settlement.client.MerchantClient;
import com.payment.settlement.client.TransactionClient;
import com.payment.settlement.dto.MerchantSummary;
import com.payment.settlement.dto.SettlementRequest;
import com.payment.settlement.dto.SettlementResponse;
import com.payment.settlement.dto.TransactionSummary;
import com.payment.settlement.model.Settlement;
import com.payment.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private static final String APPROVED_STATUS = "AUTHORIZED";
    private static final BigDecimal FEE_RATE = new BigDecimal("0.02"); // 2% acquiring fee

    private final SettlementRepository repository;
    private final TransactionClient transactionClient;
    private final MerchantClient merchantClient;

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void processSettlements() {
        log.info("Starting daily settlement process");

        List<MerchantSummary> merchants;
        try {
            merchants = merchantClient.getAllMerchants();
        } catch (Exception e) {
            log.error("Unable to reach merchant-service, aborting settlement run: {}", e.getMessage());
            return;
        }

        LocalDate today = LocalDate.now();
        int created = 0;
        int skipped = 0;

        for (MerchantSummary merchant : merchants) {
            if (repository.existsByMerchantIdAndSettlementDate(merchant.getId(), today)) {
                skipped++;
                continue;
            }
            try {
                SettlementResponse response = createSettlement(SettlementRequest.builder()
                        .merchantId(merchant.getId())
                        .settlementDate(today)
                        .build());
                if (response.getTransactionCount() != null && response.getTransactionCount() > 0) {
                    created++;
                    log.info("Settlement created for merchant {}: {} txns, ${}",
                            merchant.getId(), response.getTransactionCount(), response.getTotalAmount());
                }
            } catch (Exception e) {
                log.warn("Skipping settlement for merchant {}: {}", merchant.getId(), e.getMessage());
            }
        }

        log.info("Daily settlement process complete: {} settlement(s) created, {} merchant(s) already settled",
                created, skipped);
    }

    public SettlementResponse createSettlement(SettlementRequest request) {
        String merchantId = request.getMerchantId();
        LocalDate date = request.getSettlementDate() != null ? request.getSettlementDate() : LocalDate.now();

        if (repository.existsByMerchantIdAndSettlementDate(merchantId, date)) {
            throw new IllegalStateException(
                    "Settlement already exists for merchant " + merchantId + " on " + date);
        }

        List<TransactionSummary> approvedTransactions = fetchApprovedTransactions(merchantId, date);

        BigDecimal totalAmount = approvedTransactions.stream()
                .map(TransactionSummary::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Settlement settlement = new Settlement();
        settlement.setMerchantId(merchantId);
        settlement.setSettlementDate(date);
        settlement.setTotalAmount(totalAmount);
        settlement.setFees(totalAmount.multiply(FEE_RATE));
        settlement.setNetAmount(totalAmount.subtract(settlement.getFees()));
        settlement.setTransactionCount(approvedTransactions.size());
        settlement.setStatus(SettlementStatus.PENDING);

        settlement = repository.save(settlement);
        log.info("Settlement {} created for merchant {}: {} transaction(s), ${} gross",
                settlement.getId(), merchantId, approvedTransactions.size(), totalAmount);

        return toResponse(settlement);
    }

    public List<SettlementResponse> getMerchantSettlements(String merchantId) {
        return repository.findByMerchantId(merchantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SettlementResponse> getAllSettlements() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<SettlementResponse> getSettlementById(String id) {
        return repository.findById(id).map(this::toResponse);
    }

    private List<TransactionSummary> fetchApprovedTransactions(String merchantId, LocalDate date) {
        List<TransactionSummary> transactions;
        try {
            transactions = transactionClient.getMerchantTransactions(merchantId);
        } catch (Exception e) {
            log.warn("Unable to reach transaction-service for merchant {}: {}", merchantId, e.getMessage());
            return List.of();
        }
        if (transactions == null) {
            return List.of();
        }
        return transactions.stream()
                .filter(t -> APPROVED_STATUS.equalsIgnoreCase(t.getStatus()))
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().isEqual(date))
                .collect(Collectors.toList());
    }

    private SettlementResponse toResponse(Settlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .merchantId(settlement.getMerchantId())
                .settlementDate(settlement.getSettlementDate())
                .totalAmount(settlement.getTotalAmount())
                .fees(settlement.getFees())
                .netAmount(settlement.getNetAmount())
                .transactionCount(settlement.getTransactionCount())
                .status(settlement.getStatus())
                .createdAt(settlement.getCreatedAt())
                .build();
    }
}
