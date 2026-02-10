package com.payment.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalResponse {
    
    private String id;
    private String merchantId;
    private String terminalId;
    private String serialNumber;
    private String model;
    private String location;
    private Boolean active;
    private LocalDateTime createdAt;
}