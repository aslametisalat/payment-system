package com.payment.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDashboard {
    private String merchantId;
    private String businessName;
    
    // Today's stats
    private BigDecimal todayVolume;
    private Integer todayTransactions;
    
    // This week's stats
    private BigDecimal weekVolume;
    private Integer weekTransactions;
    
    // This month's stats
    private BigDecimal monthVolume;
    private Integer monthTransactions;
    
    // Overall stats
    private BigDecimal totalVolume;
    private Integer totalTransactions;
    private Double approvalRate;
    
    // Top performing hours
    private String peakHour;
    private BigDecimal averageTransaction;
}