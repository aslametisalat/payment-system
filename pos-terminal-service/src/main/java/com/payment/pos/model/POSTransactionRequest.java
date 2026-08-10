package com.payment.pos.model;

import com.payment.common.enums.CardReadMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "\\d{4,6}", message = "PIN must be 4-6 digits")
    private String pin;

    // Simulated card data (in real system, read from card)
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{13,19}", message = "Card number must be 13-19 digits")
    private String cardNumber;

    @NotBlank(message = "Expiry date is required")
    @Pattern(regexp = "\\d{4}", message = "Expiry date must be in YYMM format")
    private String expiryDate;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3-4 digits")
    private String cvv;
}