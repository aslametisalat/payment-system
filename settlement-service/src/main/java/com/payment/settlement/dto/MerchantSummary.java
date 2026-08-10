package com.payment.settlement.dto;

import lombok.Data;

/**
 * Lightweight mirror of merchant-service's MerchantResponse, used only for
 * deserializing the Feign response.
 */
@Data
public class MerchantSummary {
    private String id;
    private String businessName;
    private String status;
}
