package com.payment.transaction.dto;

import com.payment.common.enums.PaymentNetwork;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Mirrors network-service's RoutingResponse for deserializing the Feign
 * response. PaymentNetwork is a shared common-module enum, not something
 * specific to network-service, so it's reused directly rather than mirrored.
 */
@Data
public class RoutingResponse {
    private PaymentNetwork network;
    private String issuerId;
    private BigDecimal networkFee;
}
