package com.payment.transaction.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Mirrors network-service's RoutingRequest for the outbound Feign call.
 */
@Data
@Builder
public class RoutingRequest {
    private String cardNumber;
    private BigDecimal amount;
}
