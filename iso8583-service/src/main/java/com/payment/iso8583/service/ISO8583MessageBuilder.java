package com.payment.iso8583.service;

import com.payment.iso8583.dto.AuthorizationRequestData;
import com.payment.iso8583.dto.AuthorizationResponseData;
import com.payment.iso8583.model.ISO8583Message;
import com.payment.common.enums.MessageTypeIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class ISO8583MessageBuilder {
    
    /**
     * Build Authorization Request (0100)
     */
    public ISO8583Message buildAuthorizationRequest(AuthorizationRequestData data) {
        log.info("Building ISO 8583 Authorization Request");
        
        ISO8583Message message = new ISO8583Message();
        message.setMti(MessageTypeIndicator.AUTHORIZATION_REQUEST.getCode());
        
        // Field 2: Primary Account Number (PAN)
        message.setField(2, data.getPan());
        
        // Field 3: Processing Code (000000 = purchase from default account)
        message.setField(3, "000000");
        
        // Field 4: Transaction Amount (12 digits, no decimal)
        message.setField(4, String.format("%012d", data.getAmountInCents()));
        
        // Field 7: Transmission Date & Time (MMDDhhmmss)
        LocalDateTime now = LocalDateTime.now();
        String transmissionDateTime = now.format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        message.setField(7, transmissionDateTime);
        
        // Field 11: System Trace Audit Number (STAN)
        message.setField(11, data.getStan());
        
        // Field 12: Local Transaction Time (HHmmss)
        String localTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        message.setField(12, localTime);
        
        // Field 13: Local Transaction Date (MMDD)
        String localDate = now.format(DateTimeFormatter.ofPattern("MMdd"));
        message.setField(13, localDate);
        
        // Field 14: Card Expiration Date (YYMM)
        message.setField(14, data.getExpiryDate());
        
        // Field 22: POS Entry Mode
        message.setField(22, data.getPosEntryMode());
        
        // Field 25: POS Condition Code (00 = Normal)
        message.setField(25, "00");
        
        // Field 32: Acquiring Institution ID
        if (data.getAcquirerId() != null) {
            message.setField(32, data.getAcquirerId());
        }
        
        // Field 35: Track 2 Data
        if (data.getTrack2Data() != null) {
            message.setField(35, data.getTrack2Data());
        }
        
        // Field 37: Retrieval Reference Number
        message.setField(37, data.getRetrievalReferenceNumber());
        
        // Field 41: Card Acceptor Terminal ID
        message.setField(41, data.getTerminalId());
        
        // Field 42: Card Acceptor ID Code (Merchant ID)
        message.setField(42, data.getMerchantId());
        
        // Field 43: Card Acceptor Name/Location
        message.setField(43, data.getMerchantNameLocation());
        
        // Field 49: Transaction Currency Code
        message.setField(49, data.getCurrencyCode());
        
        // Field 52: PIN Data (Encrypted)
        if (data.getEncryptedPIN() != null) {
            message.setField(52, data.getEncryptedPIN());
        }
        
        // Field 55: ICC (Chip) Data (EMV tags)
        if (data.getEmvData() != null) {
            message.setField(55, data.getEmvData());
        }
        
        log.info("ISO 8583 message built: MTI={}, Fields={}", 
                message.getMti(), message.getDataElements().size());
        
        return message;
    }
    
    /**
     * Build Authorization Response (0110)
     */
    public ISO8583Message buildAuthorizationResponse(ISO8583Message request, 
                                                     AuthorizationResponseData data) {
        log.info("Building ISO 8583 Authorization Response");
        
        ISO8583Message response = new ISO8583Message();
        response.setMti(MessageTypeIndicator.AUTHORIZATION_RESPONSE.getCode());
        
        // Echo back fields from request
        if (request.hasField(2)) response.setField(2, request.getField(2));
        if (request.hasField(3)) response.setField(3, request.getField(3));
        if (request.hasField(4)) response.setField(4, request.getField(4));
        if (request.hasField(11)) response.setField(11, request.getField(11));
        if (request.hasField(37)) response.setField(37, request.getField(37));
        if (request.hasField(41)) response.setField(41, request.getField(41));
        if (request.hasField(42)) response.setField(42, request.getField(42));
        
        // Field 7: Transmission Date & Time
        LocalDateTime now = LocalDateTime.now();
        String transmissionDateTime = now.format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        response.setField(7, transmissionDateTime);
        
        // Field 12: Local Transaction Time
        String localTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        response.setField(12, localTime);
        
        // Field 13: Local Transaction Date
        String localDate = now.format(DateTimeFormatter.ofPattern("MMdd"));
        response.setField(13, localDate);
        
        // Field 38: Authorization ID Response (if approved)
        if (data.getAuthorizationCode() != null) {
            response.setField(38, data.getAuthorizationCode());
        }
        
        // Field 39: Response Code
        response.setField(39, data.getResponseCode());
        
        // Field 55: Issuer Authentication Data (ARPC for EMV)
        if (data.getIssuerAuthData() != null) {
            response.setField(55, data.getIssuerAuthData());
        }
        
        log.info("ISO 8583 response built: ResponseCode={}", data.getResponseCode());
        
        return response;
    }
    
    /**
     * Convert message to string for transmission
     */
    public String messageToString(ISO8583Message message) {
        StringBuilder sb = new StringBuilder();
        
        // MTI
        sb.append(message.getMti());
        
        // Bitmap
        sb.append(message.toBitmapHex());
        
        // Data elements (in order)
        for (int i = 2; i <= 128; i++) {
            if (message.hasField(i)) {
                sb.append(message.getField(i));
            }
        }
        
        return sb.toString();
    }
}