package com.payment.merchant.model;

import com.payment.common.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String businessName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String phone;
    
    @Column(nullable = false)
    private String address;
    
    @Column(nullable = false)
    private String taxId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;
    
    @Column(nullable = false)
    private String merchantCategoryCode; // MCC
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyLimit;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentDailyVolume;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal currentMonthlyVolume;
    
    // Banking details for settlement
    private String accountNumber;
    private String routingNumber;
    private String bankName;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}