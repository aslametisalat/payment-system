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
    private String cardNumber;
    
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}