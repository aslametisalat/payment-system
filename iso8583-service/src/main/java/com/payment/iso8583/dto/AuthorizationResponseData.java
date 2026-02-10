package com.payment.iso8583.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponseData {
    private String responseCode; // 00 = approved, 51 = insufficient funds, etc.
    private String authorizationCode; // 6-digit approval code
    private String issuerAuthData; // ARPC for EMV
}