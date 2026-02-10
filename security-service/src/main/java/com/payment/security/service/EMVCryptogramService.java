package com.payment.security.service;

import com.payment.common.dto.EMVTransactionData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
@Slf4j
public class EMVCryptogramService {
    
    private final SecureRandom random = new SecureRandom();
    
    /**
     * Generate ARQC (Authorization Request Cryptogram)
     */
    public String generateARQC(EMVTransactionData txnData, String cardMasterKey) {
        try {
            log.debug("Generating ARQC for amount: {}", txnData.getAmount());
            
            String cryptogramData = buildCryptogramData(txnData);
            String sessionKey = deriveSessionKey(cardMasterKey, txnData.getAtc());
            String arqc = generateCryptogram(cryptogramData, sessionKey);
            
            log.debug("ARQC generated successfully");
            return arqc;
            
        } catch (Exception e) {
            log.error("Error generating ARQC", e);
            throw new RuntimeException("ARQC generation failed", e);
        }
    }
    
    /**
     * Generate ARPC (Authorization Response Cryptogram)
     */
    public String generateARPC(String arqc, EMVTransactionData txnData, 
                              String cardMasterKey, String authorizationCode) {
        try {
            // Verify ARQC first
            String expectedARQC = generateARQC(txnData, cardMasterKey);
            
            if (!arqc.equals(expectedARQC)) {
                log.error("ARQC verification failed");
                throw new SecurityException("ARQC verification failed");
            }
            
            log.debug("ARQC verified successfully");
            
            // Generate ARPC
            String arpcData = buildARPCData(authorizationCode, txnData);
            String sessionKey = deriveSessionKey(cardMasterKey, txnData.getAtc());
            String arpc = generateCryptogram(arpcData, sessionKey);
            
            log.debug("ARPC generated successfully");
            return arpc;
            
        } catch (Exception e) {
            log.error("Error generating ARPC", e);
            throw new RuntimeException("ARPC generation failed", e);
        }
    }
    
    private String buildCryptogramData(EMVTransactionData txnData) {
        StringBuilder data = new StringBuilder();
        
        // Amount (12 digits)
        data.append(String.format("%012d", txnData.getAmount()));
        
        // Currency code (4 digits)
        data.append(String.format("%04d", txnData.getCurrencyCode()));
        
        // Transaction date (6 digits YYMMDD)
        data.append(txnData.getTransactionDate());
        
        // Transaction type (2 digits)
        data.append(String.format("%02d", txnData.getTransactionType()));
        
        // Unpredictable number (8 hex)
        data.append(txnData.getUnpredictableNumber());
        
        // ATC (4 hex)
        data.append(String.format("%04X", txnData.getAtc()));
        
        // Pad to 16 hex characters (8 bytes)
        while (data.length() < 32) {
            data.append("0");
        }
        
        return data.toString().substring(0, 32);
    }
    
    private String deriveSessionKey(String masterKey, int atc) {
        try {
            // Simplified session key derivation
            String atcHex = String.format("%04X", atc);
            String diversificationData = atcHex + "0000" + atcHex + "0000";
            
            byte[] masterKeyBytes = hexStringToByteArray(masterKey);
            byte[] diversificationBytes = hexStringToByteArray(diversificationData);
            
            // Pad diversification data to 8 bytes
            if (diversificationBytes.length < 8) {
                diversificationBytes = Arrays.copyOf(diversificationBytes, 8);
            }
            
            SecretKeySpec keySpec = new SecretKeySpec(masterKeyBytes, "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            
            byte[] sessionKeyBytes = cipher.doFinal(diversificationBytes);
            return byteArrayToHexString(sessionKeyBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Session key derivation failed", e);
        }
    }
    
    private String generateCryptogram(String data, String sessionKey) {
        try {
            byte[] dataBytes = hexStringToByteArray(data);
            byte[] keyBytes = hexStringToByteArray(sessionKey);
            
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            
            byte[] cryptogram = cipher.doFinal(dataBytes);
            
            // Return first 8 bytes as hex (16 characters)
            return byteArrayToHexString(cryptogram).substring(0, 16);
            
        } catch (Exception e) {
            throw new RuntimeException("Cryptogram generation failed", e);
        }
    }
    
    private String buildARPCData(String authCode, EMVTransactionData txnData) {
        StringBuilder data = new StringBuilder();
        data.append(authCode);
        data.append(String.format("%04X", txnData.getAtc()));
        
        // Pad to 16 hex characters
        while (data.length() < 16) {
            data.append("0");
        }
        
        return data.toString().substring(0, 16);
    }
    
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}