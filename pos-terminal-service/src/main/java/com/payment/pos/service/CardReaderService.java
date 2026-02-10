package com.payment.pos.service;

import com.payment.common.dto.CardData;
import com.payment.common.enums.CardReadMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CardReaderService {
    
    /**
     * Simulate reading card data based on read method
     */
    public CardData readCard(CardReadMethod readMethod, String cardNumber, 
                            String expiryDate, String cvv) {
        log.info("Reading card using: {}", readMethod);
        
        CardData cardData = CardData.builder()
                .pan(cardNumber)
                .expiryDate(expiryDate)
                .cvv(cvv)
                .cardType(determineCardType(cardNumber))
                .build();
        
        switch (readMethod) {
            case CHIP:
                log.debug("EMV chip card detected");
                cardData.setServiceCode("201"); // Chip card
                break;
            case MAGNETIC_STRIPE:
                log.debug("Magnetic stripe card detected");
                cardData.setTrack2Data(buildTrack2Data(cardNumber, expiryDate));
                cardData.setServiceCode("101"); // Magnetic stripe
                break;
            case CONTACTLESS:
                log.debug("Contactless card detected");
                cardData.setServiceCode("227"); // Contactless
                break;
            default:
                log.debug("Manual entry");
                cardData.setServiceCode("000");
        }
        
        log.info("Card read successfully: Type={}, Last4={}", 
                cardData.getCardType(), 
                cardNumber.substring(cardNumber.length() - 4));
        
        return cardData;
    }
    
    private String determineCardType(String pan) {
        if (pan.startsWith("4")) return "VISA";
        if (pan.startsWith("5")) return "MASTERCARD";
        if (pan.startsWith("3")) return "AMEX";
        return "UNKNOWN";
    }
    
    private String buildTrack2Data(String pan, String expiryDate) {
        // Track 2 format: PAN=Expiry+ServiceCode+Discretionary
        return pan + "=" + expiryDate + "101" + "000000000000";
    }
}