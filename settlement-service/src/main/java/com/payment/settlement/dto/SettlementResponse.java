package com.payment.settlement.dto;

import com.payment.common.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {
    private String id;
    private String merchantId;
    private LocalDate settlementDate;
    private BigDecimal totalAmount;
    private BigDecimal fees;
    private BigDecimal netAmount;
    private Integer transactionCount;
    private SettlementStatus status;
    private LocalDateTime createdAt;
}