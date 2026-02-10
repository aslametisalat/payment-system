package com.payment.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardData {
    private String pan;
    private String expiryDate;
    private String cardholderName;
    private String cardType;
    private String track2Data;
    private String cvv;
    private String serviceCode;
}