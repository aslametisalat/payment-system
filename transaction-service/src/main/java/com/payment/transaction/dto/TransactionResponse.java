package com.payment.transaction.dto;

import com.payment.common.enums.TransactionStatus;
import com.payment.common.enums.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TransactionResponse {
    private String id;
    private String merchantId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String authorizationCode;
    private String responseCode;
    private String responseMessage;
    private String stan;
    private String rrn;
    private LocalDateTime createdAt;
    // The hop-by-hop trail this transaction took - see TransactionStepDto.
    private List<TransactionStepDto> steps;
}
