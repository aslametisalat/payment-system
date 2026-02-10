package com.payment.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

@Service
@Slf4j
public class MACService {
    
    /**
     * Generate Message Authentication Code using CBC-MAC
     */
    public String generateMAC(String message, String macKey) {
        try {
            log.debug("Generating MAC for message length: {}", message.length());
            
            byte[] messageBytes = message.getBytes("UTF-8");
            byte[] paddedMessage = padMessage(messageBytes);
            byte[] mac = calculateCBCMAC(paddedMessage, macKey);
            
            // Return first 8 bytes (16 hex characters)
            String macHex = byteArrayToHexString(Arrays.copyOf(mac, 8));
            
            log.debug("MAC generated: {}****", macHex.substring(0, 4));
            return macHex;
            
        } catch (Exception e) {
            log.error("Error generating MAC", e);
            throw new RuntimeException("MAC generation failed", e);
        }
    }
    
    /**
     * Verify MAC
     */
    public boolean verifyMAC(String message, String receivedMAC, String macKey) {
        try {
            String calculatedMAC = generateMAC(message, macKey);
            boolean valid = calculatedMAC.equalsIgnoreCase(receivedMAC);
            
            log.debug("MAC verification: {}", valid ? "SUCCESS" : "FAILED");
            return valid;
            
        } catch (Exception e) {
            log.error("Error verifying MAC", e);
            return false;
        }
    }
    
    private byte[] calculateCBCMAC(byte[] data, String key) throws Exception {
        byte[] keyBytes = hexStringToByteArray(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        // CBC mode simulation
        byte[] iv = new byte[8]; // Initial vector (all zeros)
        byte[] result = iv;
        
        // Process each 8-byte block
        for (int i = 0; i < data.length; i += 8) {
            byte[] block = Arrays.copyOfRange(data, i, Math.min(i + 8, data.length));
            
            // Ensure block is 8 bytes
            if (block.length < 8) {
                block = Arrays.copyOf(block, 8);
            }
            
            // XOR with previous result
            for (int j = 0; j < 8; j++) {
                block[j] ^= result[j];
            }
            
            // Encrypt
            result = cipher.doFinal(block);
        }
        
        return result;
    }
    
    private byte[] padMessage(byte[] message) {
        // ISO/IEC 9797-1 Padding Method 2
        int paddingLength = 8 - (message.length % 8);
        if (paddingLength == 8) paddingLength = 0;
        
        byte[] padded = new byte[message.length + paddingLength];
        System.arraycopy(message, 0, padded, 0, message.length);
        
        if (paddingLength > 0) {
            padded[message.length] = (byte) 0x80;
            for (int i = message.length + 1; i < padded.length; i++) {
                padded[i] = 0x00;
            }
        }
        
        return padded;
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