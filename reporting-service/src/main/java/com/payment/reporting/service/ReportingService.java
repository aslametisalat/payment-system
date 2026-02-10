package com.payment.reporting.service;
import com.payment.reporting.dto.TransactionReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {
    
    public TransactionReport generateTransactionReport(String merchantId) {
        log.info("Generating report for merchant: {}", merchantId);
        // In real system, aggregate data from transaction service
        return TransactionReport.builder()
            .totalTransactions(100)
            .totalVolume(new BigDecimal("50000.00"))
            .approvedCount(95)
            .declinedCount(5)
            .approvalRate(new BigDecimal("95.0"))
            .build();
    }
}
