package com.payment.pos.dto;

import com.payment.common.enums.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Mirrors transaction-service's TransactionRequest for the outbound Feign
 * call. TransactionType/TransactionStatus are shared common-module enums,
 * so they're reused directly rather than mirrored.
 */
@Data
@Builder
public class TransactionRequest {
    private String merchantId;
    private String cardNumber;
    private String cvv;
    private String terminalId;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;
    private String stan;
    private String rrn;
}
