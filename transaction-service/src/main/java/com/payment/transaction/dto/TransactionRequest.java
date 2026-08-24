package com.payment.transaction.dto;

import com.payment.common.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    @NotBlank(message = "Card number is required")
    private String cardNumber;
    
    @NotBlank(message = "CVV is required")
    private String cvv;
    
    private String terminalId;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType type;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String currency = "USD";

    // Optional: lets a caller safely retry the same authorize call (e.g.
    // after a timeout where it can't tell if the first attempt landed)
    // without risking a duplicate authorization - see
    // TransactionProcessingService.processTransaction().
    private String idempotencyKey;
}
