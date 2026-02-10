package com.payment.merchant.dto;

import com.payment.common.enums.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponse {
    
    private String id;
    private String businessName;
    private String email;
    private String phone;
    private String address;
    private MerchantStatus status;
    private String merchantCategoryCode;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal currentDailyVolume;
    private BigDecimal currentMonthlyVolume;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}