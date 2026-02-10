package com.payment.common.enums;

import lombok.Getter;

@Getter
public enum MessageTypeIndicator {
    AUTHORIZATION_REQUEST("0100", "Authorization Request"),
    AUTHORIZATION_RESPONSE("0110", "Authorization Response"),
    FINANCIAL_REQUEST("0200", "Financial Transaction Request"),
    FINANCIAL_RESPONSE("0210", "Financial Transaction Response"),
    REVERSAL_REQUEST("0400", "Reversal Request"),
    REVERSAL_RESPONSE("0410", "Reversal Response"),
    NETWORK_MANAGEMENT_REQUEST("0800", "Network Management Request"),
    NETWORK_MANAGEMENT_RESPONSE("0810", "Network Management Response");
    
    private final String code;
    private final String description;
    
    MessageTypeIndicator(String code, String description) {
        this.code = code;
        this.description = description;
    }
}