package com.payment.transaction.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AuthorizationRequest {
    private String cardNumber;
    private String cvv;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
}