package com.payment.network.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRequest {
    
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{12,19}", message = "Card number must be 12-19 digits")
    private String cardNumber;
    
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}