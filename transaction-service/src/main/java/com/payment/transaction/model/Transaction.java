package com.payment.transaction.model;

import com.payment.common.enums.TransactionStatus;
import com.payment.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String merchantId;
    private String cardNumber;
    private String terminalId;
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    private BigDecimal amount;
    private String currency;
    
    private String authorizationCode;
    private String responseCode;
    private String responseMessage;
    
    private String acquirerId;
    private String issuerId;
    private String networkId;
    
    private LocalDateTime createdAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime settledAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = TransactionStatus.INITIATED;
        }
        if (currency == null) {
            currency = "USD";
        }
    }
}
