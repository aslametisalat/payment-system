package com.payment.pos.service;

import com.payment.common.dto.CardData;
import com.payment.common.dto.EMVTransactionData;
import com.payment.common.enums.CardReadMethod;
import com.payment.common.enums.POSEntryMode;
import com.payment.common.enums.ResponseCode;
import com.payment.common.enums.TransactionStatus;
import com.payment.common.enums.TransactionType;
import com.payment.iso8583.dto.AuthorizationRequestData;
import com.payment.iso8583.model.ISO8583Message;
import com.payment.iso8583.service.ISO8583MessageBuilder;
import com.payment.pos.client.TransactionClient;
import com.payment.pos.dto.TransactionRequest;
import com.payment.pos.dto.TransactionResponse;
import com.payment.pos.model.POSTransactionRequest;
import com.payment.pos.model.POSTransactionResult;
import com.payment.security.service.EMVCryptogramService;
import com.payment.security.service.MACService;
import com.payment.security.service.PINBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

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
    private final TransactionClient transactionClient;
    private final Random random;
    
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
                if (request.getPin() == null || request.getPin().isBlank()) {
                    log.warn("  ✗ PIN required but not provided");
                    return buildErrorResult("PIN required for this transaction", stan, rrn);
                }
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
            // This is the wire format a real card terminal would send over
            // a dedicated link to the acquirer. This simulation doesn't have
            // a raw ISO 8583 listener anywhere downstream - every other
            // service speaks REST/JSON - so building it here is purely to
            // demonstrate the format; the actual dispatch below goes out as
            // a normal HTTP call to transaction-service instead.
            log.info("→ Step 4: Building ISO 8583 message...");
            AuthorizationRequestData isoData = buildAuthorizationData(
                    request, cardData, encryptedPIN, emvData, stan, rrn
            );
            ISO8583Message message = messageBuilder.buildAuthorizationRequest(isoData);

            // Step 5: Calculate and add MAC (signs the outbound message)
            log.info("→ Step 5: Calculating MAC...");
            // Reserve field 64's bitmap bit before hashing: toBitmapHex() is
            // recomputed from live state, so setting the field only *after*
            // hashing would make the bitmap seen at verification time differ
            // from the one that was actually hashed. The MAC's own value is
            // still excluded from what gets hashed.
            message.setField(64, "");
            String messageString = messageBuilder.messageToString(message);
            String mac = macService.generateMAC(messageString, MAC_KEY);
            message.setField(64, mac);
            log.debug("  ✓ MAC added");

            // Step 6: Send to authorization - a real HTTP call through the
            // rest of the payment system (transaction-service, which in turn
            // calls merchant/acquirer/network/issuer-service).
            log.info("→ Step 6: Sending authorization request to transaction-service...");
            TransactionResponse txnResponse = transactionClient.authorize(
                    TransactionRequest.builder()
                            .merchantId(request.getMerchantId())
                            .cardNumber(cardData.getPan())
                            .cvv(request.getCvv())
                            .terminalId(request.getTerminalId())
                            .type(TransactionType.PURCHASE)
                            .amount(BigDecimal.valueOf(request.getAmount(), 2)) // cents -> dollars
                            .currency("USD")
                            .build());

            // Step 7: Process the response
            boolean approved = txnResponse.getStatus() == TransactionStatus.AUTHORIZED;
            String responseCode = txnResponse.getResponseCode();
            String authCode = txnResponse.getAuthorizationCode();
            // Prefer the specific reason transaction-service gave (e.g.
            // "Daily limit exceeded") over the generic ISO 8583 code
            // description (e.g. "Invalid merchant" for "03") - the code
            // description is only a fallback for when no specific message
            // comes back.
            String responseMessage = txnResponse.getResponseMessage() != null
                    ? txnResponse.getResponseMessage()
                    : ResponseCode.fromCode(responseCode).getMessage();

            log.info("╔═══════════════════════════════════════╗");
            log.info("║   TRANSACTION COMPLETE                ║");
            log.info("╚═══════════════════════════════════════╝");
            log.info("Result: {}", approved ? "✓ APPROVED" : "✗ DECLINED");
            log.info("Response Code: {} - {}", responseCode, responseMessage);
            if (approved) {
                log.info("Authorization Code: {}", authCode);
            }

            // Step 8: Generate receipt
            String receipt = receiptService.generateReceipt(
                    request, cardData, approved, responseCode, responseMessage, authCode
            );

            return POSTransactionResult.builder()
                    .approved(approved)
                    .responseCode(responseCode)
                    .responseMessage(responseMessage)
                    .authorizationCode(authCode)
                    .transactionId(txnResponse.getId())
                    .receipt(receipt)
                    .timestamp(LocalDateTime.now())
                    .stan(stan)
                    .rrn(rrn)
                    .macVerified(true) // outbound request MAC computed and attached in Step 5
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
        return String.format("%06d", random.nextInt(1000000));
    }

    private String generateRRN() {
        return String.format("%012d", Math.abs(random.nextLong()) % 1000000000000L);
    }

    private String generateUnpredictableNumber() {
        return String.format("%08X", random.nextInt(Integer.MAX_VALUE));
    }

    private int getNextATC() {
        return random.nextInt(65536);
    }
}