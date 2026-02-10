package com.payment.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EMVTransactionData {
    private Long amount; // in cents
    private Integer currencyCode; // 840 for USD
    private String transactionDate; // YYMMDD
    private Integer transactionType; // 00 for purchase
    private String unpredictableNumber; // 8 hex digits
    private Integer atc; // Application Transaction Counter
    private String terminalCountryCode;
    private String cvmResults;
}