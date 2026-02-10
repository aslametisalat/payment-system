package com.payment.issuer.dto;

import com.payment.common.enums.CardType;
import com.payment.common.enums.PaymentNetwork;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CardResponse {
    private String id;
    private String cardNumber;
    private String cardholderName;
    private LocalDate expiryDate;
    private CardType cardType;
    private PaymentNetwork network;
    private BigDecimal creditLimit;
    private BigDecimal availableBalance;
    private boolean active;
}
