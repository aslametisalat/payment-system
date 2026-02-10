package com.payment.issuer.dto;

import com.payment.common.enums.CardType;
import com.payment.common.enums.PaymentNetwork;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardRequest {
    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;
    
    @NotNull(message = "Card type is required")
    private CardType cardType;
    
    private PaymentNetwork network = PaymentNetwork.VISA;
    private String accountId;
    private BigDecimal creditLimit;
}
