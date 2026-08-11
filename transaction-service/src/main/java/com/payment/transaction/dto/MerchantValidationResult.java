package com.payment.transaction.dto;

import lombok.Data;

/**
 * Lightweight mirror of merchant-service's MerchantService.ValidationResult,
 * used only for deserializing the Feign response. The embedded full merchant
 * record isn't needed here - Jackson's default lenient deserialization just
 * drops it.
 */
@Data
public class MerchantValidationResult {
    private boolean valid;
    private String message;
}
