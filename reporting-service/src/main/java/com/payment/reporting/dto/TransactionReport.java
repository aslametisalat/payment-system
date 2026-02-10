package com.payment.reporting.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TransactionReport {
    private long totalTransactions;
    private BigDecimal totalVolume;
    private long approvedCount;
    private long declinedCount;
    private BigDecimal approvalRate;
}
