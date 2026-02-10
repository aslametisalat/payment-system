package com.payment.issuer.model;

import com.payment.common.enums.CardType;
import com.payment.common.enums.PaymentNetwork;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String cardNumber;
    private String cardholderName;
    private String cvv;
    private LocalDate expiryDate;
    
    @Enumerated(EnumType.STRING)
    private CardType cardType;
    
    @Enumerated(EnumType.STRING)
    private PaymentNetwork network;
    
    private String accountId;
    private BigDecimal creditLimit;
    private BigDecimal availableBalance;
    private boolean active;
    private boolean blocked;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastUsed;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        blocked = false;
        if (cardNumber == null) {
            cardNumber = generateCardNumber();
        }
        if (cvv == null) {
            cvv = generateCVV();
        }
        if (expiryDate == null) {
            expiryDate = LocalDate.now().plusYears(3);
        }
        if (availableBalance == null) {
            availableBalance = creditLimit != null ? creditLimit : BigDecimal.ZERO;
        }
    }
    
    private String generateCardNumber() {
        return "4" + String.format("%015d", (long)(Math.random() * 1000000000000000L));
    }
    
    private String generateCVV() {
        return String.format("%03d", (int)(Math.random() * 1000));
    }
}
