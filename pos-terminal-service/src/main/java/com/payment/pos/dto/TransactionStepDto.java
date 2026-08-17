package com.payment.pos.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Mirrors transaction-service's TransactionStepDto for deserializing the
 * Feign response - plain @Data (no @Builder) so Jackson gets the no-args
 * constructor it needs.
 */
@Data
public class TransactionStepDto {
    private String stepName;
    private String target;
    private String status;
    private String detail;
    private Long durationMs;
    private LocalDateTime timestamp;
}
