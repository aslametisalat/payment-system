package com.payment.issuer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecureAuthorizationRequest {
    private String cardNumber;
    private String cvv;
    private String expiryDate;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
    private String encryptedPIN;
    private String emvData;
    private String mac;
}