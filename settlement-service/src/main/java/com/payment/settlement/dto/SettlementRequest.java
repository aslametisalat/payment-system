package com.payment.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRequest {
    
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    private LocalDate settlementDate;
}