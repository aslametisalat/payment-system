package com.payment.reporting.service;

import com.payment.reporting.client.MerchantClient;
import com.payment.reporting.client.TransactionClient;
import com.payment.reporting.dto.MerchantDashboard;
import com.payment.reporting.dto.MerchantSummary;
import com.payment.reporting.dto.TransactionReport;
import com.payment.reporting.dto.TransactionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private static final String APPROVED_STATUS = "AUTHORIZED";

    private final TransactionClient transactionClient;
    private final MerchantClient merchantClient;

    public TransactionReport generateTransactionReport(String merchantId) {
        return generateTransactionReport(merchantId, null, null);
    }

    public TransactionReport generateTransactionReport(String merchantId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating report for merchant: {}", merchantId);

        List<TransactionSummary> transactions = fetchTransactions(merchantId, startDate, endDate);

        long total = transactions.size();
        long approved = transactions.stream().filter(this::isApproved).count();
        long declined = total - approved;
        BigDecimal volume = sumAmount(transactions.stream().filter(this::isApproved).collect(Collectors.toList()));
        BigDecimal approvalRate = approvalRate(approved, total);

        return TransactionReport.builder()
                .totalTransactions(total)
                .totalVolume(volume)
                .approvedCount(approved)
                .declinedCount(declined)
                .approvalRate(approvalRate)
                .build();
    }

    public MerchantDashboard getMerchantDashboard(String merchantId) {
        log.info("Building dashboard for merchant: {}", merchantId);

        List<TransactionSummary> all = fetchTransactions(merchantId, null, null);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDate monthStart = today.withDayOfMonth(1);

        String businessName = fetchBusinessName(merchantId);

        List<TransactionSummary> approvedTxns = all.stream().filter(this::isApproved).collect(Collectors.toList());
        BigDecimal totalVolume = sumAmount(approvedTxns);
        double approvalRate = all.isEmpty() ? 0.0 : (100.0 * approvedTxns.size() / all.size());
        BigDecimal averageTransaction = approvedTxns.isEmpty()
                ? BigDecimal.ZERO
                : totalVolume.divide(new BigDecimal(approvedTxns.size()), 2, RoundingMode.HALF_UP);

        return MerchantDashboard.builder()
                .merchantId(merchantId)
                .businessName(businessName)
                .todayVolume(sumInRange(approvedTxns, today, today))
                .todayTransactions((int) countInRange(approvedTxns, today, today))
                .weekVolume(sumInRange(approvedTxns, weekStart, today))
                .weekTransactions((int) countInRange(approvedTxns, weekStart, today))
                .monthVolume(sumInRange(approvedTxns, monthStart, today))
                .monthTransactions((int) countInRange(approvedTxns, monthStart, today))
                .totalVolume(totalVolume)
                .totalTransactions(all.size())
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .peakHour(peakHour(all))
                .averageTransaction(averageTransaction)
                .build();
    }

    private List<TransactionSummary> fetchTransactions(String merchantId, LocalDate startDate, LocalDate endDate) {
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
        if (startDate == null && endDate == null) {
            return transactions;
        }
        return transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> {
                    LocalDate d = t.getCreatedAt().toLocalDate();
                    boolean afterStart = startDate == null || !d.isBefore(startDate);
                    boolean beforeEnd = endDate == null || !d.isAfter(endDate);
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
    }

    private String fetchBusinessName(String merchantId) {
        try {
            MerchantSummary merchant = merchantClient.getMerchant(merchantId);
            return merchant != null ? merchant.getBusinessName() : null;
        } catch (Exception e) {
            log.warn("Unable to reach merchant-service for merchant {}: {}", merchantId, e.getMessage());
            return null;
        }
    }

    private boolean isApproved(TransactionSummary t) {
        return APPROVED_STATUS.equalsIgnoreCase(t.getStatus());
    }

    private BigDecimal sumAmount(List<TransactionSummary> transactions) {
        return transactions.stream()
                .map(TransactionSummary::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumInRange(List<TransactionSummary> transactions, LocalDate from, LocalDate to) {
        return sumAmount(inRange(transactions, from, to));
    }

    private long countInRange(List<TransactionSummary> transactions, LocalDate from, LocalDate to) {
        return inRange(transactions, from, to).size();
    }

    private List<TransactionSummary> inRange(List<TransactionSummary> transactions, LocalDate from, LocalDate to) {
        return transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> {
                    LocalDate d = t.getCreatedAt().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                })
                .collect(Collectors.toList());
    }

    private BigDecimal approvalRate(long approved, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(approved)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
    }

    private String peakHour(List<TransactionSummary> transactions) {
        Map<Integer, Long> byHour = transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .collect(Collectors.groupingBy(t -> t.getCreatedAt().getHour(), Collectors.counting()));

        return byHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> String.format("%02d:00 - %02d:00", e.getKey(), (e.getKey() + 1) % 24))
                .orElse("N/A");
    }
}
