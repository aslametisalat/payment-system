package com.payment.network.dto;

import com.payment.common.enums.PaymentNetwork;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingResponse {
    private PaymentNetwork network;
    private String issuerId;
    private BigDecimal networkFee;
}