package com.payment.pos.model;

import com.payment.common.enums.CardReadMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POSTransactionRequest {
    @NotBlank(message = "Terminal ID is required")
    private String terminalId;
    
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    @NotBlank(message = "Merchant name is required")
    private String merchantName;
    
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Long amount; // in cents
    
    @NotNull(message = "Card read method is required")
    private CardReadMethod cardReadMethod;
    
    private Boolean requirePIN = true;
    
    // For simulation - in real system this comes from PIN pad
    private String pin;
    
    // Simulated card data (in real system, read from card)
    private String cardNumber;
    private String expiryDate;
    private String cvv;
}