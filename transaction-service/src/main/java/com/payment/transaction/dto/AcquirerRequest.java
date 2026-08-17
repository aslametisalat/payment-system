package com.payment.transaction.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Mirrors acquirer-service's AcquirerRequest for the outbound Feign call.
 */
@Data
@Builder
public class AcquirerRequest {
    private String merchantId;
    private String cardNumber;
    private BigDecimal amount;
    private String terminalId;
}
