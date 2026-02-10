package com.payment.settlement.model;
import com.payment.common.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String merchantId;
    private LocalDate settlementDate;
    private BigDecimal totalAmount;
    private BigDecimal fees;
    private BigDecimal netAmount;
    private int transactionCount;
    @Enumerated(EnumType.STRING)
    private SettlementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = SettlementStatus.PENDING;
    }
}
