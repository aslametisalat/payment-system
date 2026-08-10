package com.payment.network.service;

import com.payment.common.enums.PaymentNetwork;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class NetworkRoutingService {
    
    /**
     * Determine card network and route to issuer
     */
    public RoutingResult route(String cardNumber, BigDecimal amount) {
        log.info("Routing transaction for card: ****{}", maskLast4(cardNumber));
        
        // Determine network from BIN (first 6 digits)
        PaymentNetwork network = determineNetwork(cardNumber);
        log.info("Card network: {}", network);
        
        // Calculate network fee
        BigDecimal networkFee = calculateNetworkFee(amount, network);
        log.debug("Network fee: ${}", networkFee);
        
        // Get issuer ID from BIN
        String issuerId = lookupIssuer(cardNumber);
        log.info("Routing to issuer: {}", issuerId);
        
        return RoutingResult.builder()
                .network(network)
                .issuerId(issuerId)
                .networkFee(networkFee)
                .build();
    }
    
    private PaymentNetwork determineNetwork(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return PaymentNetwork.LOCAL;
        }
        String firstDigit = cardNumber.substring(0, 1);

        switch (firstDigit) {
            case "4":
                return PaymentNetwork.VISA;
            case "5":
                return PaymentNetwork.MASTERCARD;
            case "3":
                return PaymentNetwork.AMEX;
            default:
                return PaymentNetwork.LOCAL;
        }
    }
    
    private BigDecimal calculateNetworkFee(BigDecimal amount, PaymentNetwork network) {
        // Typical network interchange fees
        BigDecimal feePercent = switch (network) {
            case VISA -> new BigDecimal("0.0015"); // 0.15%
            case MASTERCARD -> new BigDecimal("0.0014"); // 0.14%
            case AMEX -> new BigDecimal("0.0025"); // 0.25%
            default -> new BigDecimal("0.001"); // 0.10%
        };
        
        return amount.multiply(feePercent);
    }
    
    private String lookupIssuer(String cardNumber) {
        // In real system, BIN lookup to database
        // For simulation, return generic issuer
        if (cardNumber == null || cardNumber.length() < 6) {
            return "ISSUER-UNKNOWN";
        }
        return "ISSUER-" + cardNumber.substring(0, 6);
    }

    private String maskLast4(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return cardNumber.substring(cardNumber.length() - 4);
    }

    @lombok.Data
    @lombok.Builder
    public static class RoutingResult {
        private PaymentNetwork network;
        private String issuerId;
        private BigDecimal networkFee;
    }
}