package com.payment.iso8583.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequestData {
    private String pan; // Primary Account Number
    private Long amountInCents;
    private String expiryDate; // YYMM
    private String posEntryMode; // 051 = chip with PIN
    private String stan; // System Trace Audit Number
    private String acquirerId;
    private String track2Data;
    private String retrievalReferenceNumber;
    private String terminalId;
    private String merchantId;
    private String merchantNameLocation;
    private String currencyCode; // 840 = USD
    private String encryptedPIN;
    private String emvData; // ICC data (Field 55)
}