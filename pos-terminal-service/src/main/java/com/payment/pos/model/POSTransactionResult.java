package com.payment.pos.model;

import com.payment.pos.dto.TransactionStepDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

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

    // The hop-by-hop trail this transaction took across merchant, acquirer,
    // network and issuer services - null when the request never reached
    // transaction-service (e.g. it was unreachable).
    private List<TransactionStepDto> steps;
}