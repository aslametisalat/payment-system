package com.payment.issuer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_accounts")
@Data
@NoArgsConstructor
public class CardAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String customerId;
    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (accountNumber == null) {
            accountNumber = "ACC" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
