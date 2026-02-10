package com.payment.merchant.dto;

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
public class MerchantRequest {
    
    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    private String businessName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be valid")
    private String phone;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    @NotBlank(message = "Tax ID is required")
    private String taxId;
    
    @NotBlank(message = "Merchant Category Code is required")
    @Pattern(regexp = "^\\d{4}$", message = "MCC must be 4 digits")
    private String merchantCategoryCode;
    
    @NotNull(message = "Daily limit is required")
    @DecimalMin(value = "0.01", message = "Daily limit must be greater than 0")
    private BigDecimal dailyLimit;
    
    @NotNull(message = "Monthly limit is required")
    @DecimalMin(value = "0.01", message = "Monthly limit must be greater than 0")
    private BigDecimal monthlyLimit;
    
    // Banking details
    private String accountNumber;
    private String routingNumber;
    private String bankName;
}