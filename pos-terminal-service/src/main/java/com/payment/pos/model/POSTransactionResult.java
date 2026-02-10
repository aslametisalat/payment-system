package com.payment.pos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POSTransactionResult {
    private Boolean approved;
    private String responseCode;
    private String responseMessage;
    private String authorizationCode;
    private String transactionId;
    private String receipt;
    private String errorMessage;
    private LocalDateTime timestamp;
    
    // Security details (for logging/audit)
    private String stan;
    private String rrn;
    private Boolean macVerified;
    private Boolean pinVerified;
    private Boolean emvVerified;
}