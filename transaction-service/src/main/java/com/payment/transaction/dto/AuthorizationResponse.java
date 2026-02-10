package com.payment.transaction.dto;

import lombok.Data;

@Data
public class AuthorizationResponse {
    private boolean approved;
    private String authorizationCode;
    private String responseCode;
    private String message;
}