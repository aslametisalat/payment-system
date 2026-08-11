package com.payment.transaction.dto;

import lombok.Data;

/**
 * Mirrors acquirer-service's AcquirerResponse for deserializing the Feign
 * response - plain @Data (no @Builder) so Jackson gets the no-args
 * constructor it needs.
 */
@Data
public class AcquirerResponse {
    private Boolean approved;
    private String message;
    private String acquirerId;
    private Integer fraudScore;
}
