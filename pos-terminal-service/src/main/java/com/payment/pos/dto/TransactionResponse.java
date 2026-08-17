package com.payment.pos.dto;

import com.payment.common.enums.TransactionStatus;
import com.payment.common.enums.TransactionType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mirrors transaction-service's TransactionResponse for deserializing the
 * Feign response - plain @Data (no @Builder) so Jackson gets the no-args
 * constructor it needs.
 */
@Data
public class TransactionResponse {
    private String id;
    private String merchantId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String authorizationCode;
    private String responseCode;
    private String responseMessage;
    private LocalDateTime createdAt;
    private List<TransactionStepDto> steps;
}
