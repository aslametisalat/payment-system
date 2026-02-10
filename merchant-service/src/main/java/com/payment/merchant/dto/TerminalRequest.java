package com.payment.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalRequest {
    
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    @NotBlank(message = "Terminal ID is required")
    private String terminalId;
    
    @NotBlank(message = "Serial number is required")
    private String serialNumber;
    
    private String model;
    private String location;
}