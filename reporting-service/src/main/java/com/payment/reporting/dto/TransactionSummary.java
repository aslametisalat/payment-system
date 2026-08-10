package com.payment.reporting.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight mirror of transaction-service's TransactionResponse, used only
 * for deserializing the Feign response. Status is kept as a plain String so
 * this module doesn't need a compile-time dependency on the transaction
 * service's enum types.
 */
@Data
public class TransactionSummary {
    private String id;
    private String merchantId;
    private String type;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String authorizationCode;
    private String responseMessage;
    private LocalDateTime createdAt;
}
