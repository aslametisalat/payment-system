package com.payment.issuer.service;

import com.payment.issuer.dto.*;
import com.payment.issuer.model.Card;
import com.payment.issuer.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.payment.security.service.PINBlockService;
import com.payment.security.service.EMVCryptogramService;
import com.payment.security.service.MACService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {
    private final CardRepository cardRepository;
    
    private final PINBlockService pinBlockService;
    private final EMVCryptogramService emvService;
    private final MACService macService;

    private static final String PIN_DECRYPTION_KEY = "0123456789ABCDEF";
    private static final String MAC_KEY = "FEDCBA9876543210";

    
    @Transactional
    public CardResponse issueCard(CardRequest request) {
        log.info("Issuing new card for: {}", request.getCardholderName());
        
        Card card = new Card();
        card.setCardholderName(request.getCardholderName());
        card.setCardType(request.getCardType());
        card.setNetwork(request.getNetwork());
        card.setAccountId(request.getAccountId());
        card.setCreditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : new BigDecimal("5000"));
        
        Card saved = cardRepository.save(card);
        log.info("Card issued: {}", saved.getCardNumber());
        
        return toResponse(saved);
    }
    
    public CardResponse getCard(String id) {
        Card card = cardRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Card not found: " + id));
        return toResponse(card);
    }
    
    public List<CardResponse> getAllCards() {
        return cardRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public AuthorizationResponse authorizeTransaction(AuthorizationRequest request) {
        log.info("Authorizing transaction for card: {}", maskCardNumber(request.getCardNumber()));
        
        Card card = cardRepository.findByCardNumber(request.getCardNumber())
            .orElseThrow(() -> new RuntimeException("Card not found"));
        
        // Validate card
        if (!card.isActive()) {
            return buildDeclinedResponse("Card is not active");
        }
        
        if (card.isBlocked()) {
            return buildDeclinedResponse("Card is blocked");
        }
        
        // Validate CVV
        if (!card.getCvv().equals(request.getCvv())) {
            return buildDeclinedResponse("Invalid CVV");
        }
        
        // Check balance/limit
        if (card.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            return buildDeclinedResponse("Insufficient funds");
        }
        
        // Approve and hold funds
        card.setAvailableBalance(card.getAvailableBalance().subtract(request.getAmount()));
        cardRepository.save(card);
        
        String authCode = generateAuthCode();
        log.info("Transaction authorized: {}", authCode);
        
        return AuthorizationResponse.builder()
            .approved(true)
            .authorizationCode(authCode)
            .responseCode("00")
            .message("Approved")
            .transactionId(UUID.randomUUID().toString())
            .build();
    }
    
    @Transactional
    public void blockCard(String cardId) {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found"));
        card.setBlocked(true);
        cardRepository.save(card);
        log.info("Card blocked: {}", cardId);
    }
    
    private CardResponse toResponse(Card card) {
        return CardResponse.builder()
            .id(card.getId())
            .cardNumber(maskCardNumber(card.getCardNumber()))
            .cardholderName(card.getCardholderName())
            .expiryDate(card.getExpiryDate())
            .cardType(card.getCardType())
            .network(card.getNetwork())
            .creditLimit(card.getCreditLimit())
            .availableBalance(card.getAvailableBalance())
            .active(card.isActive())
            .build();
    }
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) return cardNumber;
        return cardNumber.substring(0, 4) + "********" + cardNumber.substring(12);
    }
    
    private AuthorizationResponse buildDeclinedResponse(String reason) {
        return AuthorizationResponse.builder()
            .approved(false)
            .authorizationCode(null)
            .responseCode("05")
            .message(reason)
            .transactionId(null)
            .build();
    }
    
    private String generateAuthCode() {
        return String.format("%06d", (int)(Math.random() * 1000000));
    }
    
    /**
     * Enhanced authorization with security verification
     */
   @Transactional
   public AuthorizationResponse authorizeTransactionSecure(AuthorizationRequest request) {
       log.info("Processing secure authorization");
       
       // 1. Verify MAC if present
       if (request.getMac() != null) {
           boolean macValid = macService.verifyMAC(
               buildMessageForMAC(request),
               request.getMac(),
               MAC_KEY
           );
           
           if (!macValid) {
               log.error("MAC verification failed");
               return buildDeclinedResponse("63", "Security violation");
           }
           log.debug("MAC verified successfully");
       }
       
       // 2. Get card
       Card card = cardRepository.findByCardNumber(request.getCardNumber())
           .orElseThrow(() -> new RuntimeException("Card not found"));
       
       // 3. Verify PIN if present
       if (request.getEncryptedPIN() != null) {
           boolean pinValid = pinBlockService.verifyPIN(
               request.getEncryptedPIN(),
               PIN_DECRYPTION_KEY,
               card.getCvv(), // Using CVV as PIN for simulation
               request.getCardNumber()
           );
           
           if (!pinValid) {
               log.error("PIN verification failed");
               return buildDeclinedResponse("55", "Incorrect PIN");
           }
           log.debug("PIN verified successfully");
       }
       
       // 4. Check card status
       if (!card.isActive()) {
           return buildDeclinedResponse("54", "Card not active");
       }
       
       if (card.isBlocked()) {
           return buildDeclinedResponse("43", "Card blocked");
       }
       
       // 5. Check balance
       if (card.getAvailableBalance().compareTo(request.getAmount()) < 0) {
           return buildDeclinedResponse("51", "Insufficient funds");
       }
       
       // 6. Hold funds
       card.setAvailableBalance(card.getAvailableBalance().subtract(request.getAmount()));
       cardRepository.save(card);
       
       // 7. Generate authorization code
       String authCode = generateAuthCode();
       
       log.info("Transaction authorized: {}", authCode);
       
       return buildApprovedResponse(authCode);
   }

   private String buildMessageForMAC(AuthorizationRequest request) {
       return request.getCardNumber() + request.getAmount() + request.getMerchantId();
   }

   private AuthorizationResponse buildApprovedResponse(String authCode) {
       return AuthorizationResponse.builder()
           .approved(true)
           .authorizationCode(authCode)
           .responseCode("00")
           .message("Approved")
           .transactionId(UUID.randomUUID().toString())
           .build();
   }

   private AuthorizationResponse buildDeclinedResponse(String code, String message) {
       return AuthorizationResponse.builder()
           .approved(false)
           .authorizationCode(null)
           .responseCode(code)
           .message(message)
           .transactionId(UUID.randomUUID().toString())
           .build();
   }
}
