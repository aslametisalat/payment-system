package com.payment.pos.service;

import com.payment.common.dto.CardData;
import com.payment.common.dto.EMVTransactionData;
import com.payment.common.enums.CardReadMethod;
import com.payment.common.enums.POSEntryMode;
import com.payment.common.enums.ResponseCode;
import com.payment.iso8583.dto.AuthorizationRequestData;
import com.payment.iso8583.dto.AuthorizationResponseData;
import com.payment.iso8583.model.ISO8583Message;
import com.payment.iso8583.service.ISO8583MessageBuilder;
import com.payment.pos.model.POSTransactionRequest;
import com.payment.pos.model.POSTransactionResult;
import com.payment.security.service.EMVCryptogramService;
import com.payment.security.service.MACService;
import com.payment.security.service.PINBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class POSTransactionService {
    
    private final ISO8583MessageBuilder messageBuilder;
    private final PINBlockService pinBlockService;
    private final MACService macService;
    private final EMVCryptogramService emvService;
    private final CardReaderService cardReader;
    private final ReceiptService receiptService;
    
    // Encryption keys (in real system, these come from HSM)
    private static final String PIN_ENCRYPTION_KEY = "0123456789ABCDEF";
    private static final String MAC_KEY = "FEDCBA9876543210";
    private static final String CARD_MASTER_KEY = "0123456789ABCDEF";
    
    /**
     * Process complete POS transaction with full security
     */
    public POSTransactionResult processTransaction(POSTransactionRequest request) {
        log.info("╔═══════════════════════════════════════╗");
        log.info("║   STARTING POS TRANSACTION           ║");
        log.info("╚═══════════════════════════════════════╝");
        log.info("Merchant: {}", request.getMerchantId());
        log.info("Terminal: {}", request.getTerminalId());
        log.info("Amount: ${}", request.getAmount() / 100.0);
        
        String stan = generateSTAN();
        String rrn = generateRRN();
        
        try {
            // Step 1: Read Card
            log.info("→ Step 1: Reading card...");
            CardData cardData = cardReader.readCard(
                    request.getCardReadMethod(),
                    request.getCardNumber(),
                    request.getExpiryDate(),
                    request.getCvv()
            );
            
            // Step 2: Get PIN (if required)
            String encryptedPIN = null;
            if (Boolean.TRUE.equals(request.getRequirePIN())) {
                log.info("→ Step 2: Processing PIN...");
                String pinBlock = pinBlockService.formatPINBlock(
                        request.getPin(), 
                        cardData.getPan()
                );
                encryptedPIN = pinBlockService.encryptPINBlock(pinBlock, PIN_ENCRYPTION_KEY);
                log.debug("  ✓ PIN encrypted");
            }
            
            // Step 3: Generate EMV data (if chip card)
            String emvData = null;
            if (request.getCardReadMethod() == CardReadMethod.CHIP) {
                log.info("→ Step 3: Generating EMV cryptogram...");
                emvData = generateEMVData(request.getAmount());
                log.debug("  ✓ ARQC generated");
            }
            
            // Step 4: Build ISO 8583 message
            log.info("→ Step 4: Building ISO 8583 message...");
            AuthorizationRequestData isoData = buildAuthorizationData(
                    request, cardData, encryptedPIN, emvData, stan, rrn
            );
            ISO8583Message message = messageBuilder.buildAuthorizationRequest(isoData);
            
            // Step 5: Calculate and add MAC
            log.info("→ Step 5: Calculating MAC...");
            String messageString = messageBuilder.messageToString(message);
            String mac = macService.generateMAC(messageString, MAC_KEY);
            message.setField(64, mac);
            log.debug("  ✓ MAC added");
            
            // Step 6: Send to authorization (simulated)
            log.info("→ Step 6: Sending authorization request...");
            ISO8583Message response = sendToAuthorization(message);
            
            // Step 7: Verify response MAC
            log.info("→ Step 7: Verifying response...");
            boolean macVerified = verifyResponseMAC(response);
            
            if (!macVerified) {
                log.error("  ✗ Response MAC verification failed!");
                return buildErrorResult("Security violation - Invalid MAC", stan, rrn);
            }
            log.debug("  ✓ Response MAC verified");
            
            // Step 8: Process response
            String responseCode = response.getField(39);
            String authCode = response.getField(38);
            boolean approved = "00".equals(responseCode);
            
            ResponseCode rc = ResponseCode.fromCode(responseCode);
            
            log.info("╔═══════════════════════════════════════╗");
            log.info("║   TRANSACTION COMPLETE                ║");
            log.info("╚═══════════════════════════════════════╝");
            log.info("Result: {}", approved ? "✓ APPROVED" : "✗ DECLINED");
            log.info("Response Code: {} - {}", responseCode, rc.getMessage());
            if (approved) {
                log.info("Authorization Code: {}", authCode);
            }
            
            // Step 9: Generate receipt
            String receipt = receiptService.generateReceipt(
                    request, cardData, approved, responseCode, rc.getMessage(), authCode
            );
            
            return POSTransactionResult.builder()
                    .approved(approved)
                    .responseCode(responseCode)
                    .responseMessage(rc.getMessage())
                    .authorizationCode(authCode)
                    .transactionId(UUID.randomUUID().toString())
                    .receipt(receipt)
                    .timestamp(LocalDateTime.now())
                    .stan(stan)
                    .rrn(rrn)
                    .macVerified(macVerified)
                    .pinVerified(request.getRequirePIN())
                    .emvVerified(request.getCardReadMethod() == CardReadMethod.CHIP)
                    .build();
            
        } catch (Exception e) {
            log.error("Transaction failed with exception", e);
            return buildErrorResult(e.getMessage(), stan, rrn);
        }
    }
    
    private String generateEMVData(Long amount) {
        EMVTransactionData emvTxn = EMVTransactionData.builder()
                .amount(amount)
                .currencyCode(840) // USD
                .transactionDate(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyMMdd")))
                .transactionType(0)
                .unpredictableNumber(generateUnpredictableNumber())
                .atc(getNextATC())
                .build();
        
        String arqc = emvService.generateARQC(emvTxn, CARD_MASTER_KEY);
        return buildEMVTags(arqc, emvTxn);
    }
    
    private String buildEMVTags(String arqc, EMVTransactionData txnData) {
        StringBuilder tags = new StringBuilder();
        
        // Tag 9F26: Application Cryptogram (ARQC)
        tags.append("9F2608").append(arqc);
        
        // Tag 9F36: Application Transaction Counter
        tags.append("9F3602").append(String.format("%04X", txnData.getAtc()));
        
        // Tag 9F37: Unpredictable Number
        tags.append("9F3704").append(txnData.getUnpredictableNumber());
        
        return tags.toString();
    }
    
    private AuthorizationRequestData buildAuthorizationData(
            POSTransactionRequest request, CardData cardData, 
            String encryptedPIN, String emvData, String stan, String rrn) {
        
        return AuthorizationRequestData.builder()
                .pan(cardData.getPan())
                .amountInCents(request.getAmount())
                .expiryDate(cardData.getExpiryDate())
                .posEntryMode(getPOSEntryMode(request.getCardReadMethod()))
                .stan(stan)
                .acquirerId("123456")
                .retrievalReferenceNumber(rrn)
                .terminalId(request.getTerminalId())
                .merchantId(request.getMerchantId())
                .merchantNameLocation(request.getMerchantName())
                .currencyCode("840")
                .encryptedPIN(encryptedPIN)
                .emvData(emvData)
                .build();
    }
    
    private ISO8583Message sendToAuthorization(ISO8583Message message) {
        // In real system, this sends to issuer via acquirer
        // For simulation, create a response
        
        AuthorizationResponseData responseData = AuthorizationResponseData.builder()
                .responseCode("00")
                .authorizationCode(generateAuthCode())
                .build();
        
        return messageBuilder.buildAuthorizationResponse(message, responseData);
    }
    
    private boolean verifyResponseMAC(ISO8583Message response) {
        String responseMAC = response.getField(64);
        if (responseMAC == null) return false;
        
        String messageWithoutMAC = messageBuilder.messageToString(response)
                .replace(responseMAC, "");
        
        return macService.verifyMAC(messageWithoutMAC, responseMAC, MAC_KEY);
    }
    
    private POSTransactionResult buildErrorResult(String error, String stan, String rrn) {
        return POSTransactionResult.builder()
                .approved(false)
                .responseCode("96")
                .responseMessage("System malfunction")
                .errorMessage(error)
                .timestamp(LocalDateTime.now())
                .stan(stan)
                .rrn(rrn)
                .build();
    }
    
    private String getPOSEntryMode(CardReadMethod method) {
        switch (method) {
            case CHIP: return "051";
            case MAGNETIC_STRIPE: return "021";
            case CONTACTLESS: return "071";
            default: return "012";
        }
    }
    
    private String generateSTAN() {
        return String.format("%06d", (int)(Math.random() * 1000000));
    }
    
    private String generateRRN() {
        return String.format("%012d", (long)(Math.random() * 1000000000000L));
    }
    
    private String generateAuthCode() {
        return String.format("%06d", (int)(Math.random() * 1000000));
    }
    
    private String generateUnpredictableNumber() {
        return String.format("%08X", (int)(Math.random() * 0xFFFFFFFF));
    }
    
    private int getNextATC() {
        return (int)(Math.random() * 65535);
    }
}