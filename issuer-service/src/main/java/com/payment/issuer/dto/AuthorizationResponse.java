package com.payment.issuer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizationResponse {
    private boolean approved;
    private String authorizationCode;
    private String responseCode;
    private String message;
    private String transactionId;
    private String arpc; // Authorization Response Cryptogram (EMV)
}
