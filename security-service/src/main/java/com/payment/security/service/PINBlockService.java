package com.payment.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Service
@Slf4j
public class PINBlockService {
    
    /**
     * Format PIN Block according to ISO 9564-1 Format 0
     * Format: 0 + PIN_Length + PIN + Padding (F's)
     * Then XOR with: 0000 + last 12 digits of PAN
     */
    public String formatPINBlock(String pin, String pan) {
        try {
            log.debug("Formatting PIN block for PAN ending: {}", pan.substring(pan.length() - 4));
            
            // Step 1: Format PIN
            StringBuilder pinBlock = new StringBuilder("0");
            pinBlock.append(pin.length());
            pinBlock.append(pin);
            
            // Pad with F's to make 16 hex characters
            while (pinBlock.length() < 16) {
                pinBlock.append("F");
            }
            
            // Step 2: Format PAN
            // Take last 13 digits (excluding check digit), prepend with 0000
            String panPart = pan.substring(pan.length() - 13, pan.length() - 1);
            String panBlock = "0000" + panPart;
            
            // Step 3: XOR the two blocks
            String result = xorHexStrings(pinBlock.toString(), panBlock);
            
            log.debug("PIN block formatted successfully");
            return result;
            
        } catch (Exception e) {
            log.error("Error formatting PIN block", e);
            throw new RuntimeException("PIN block formatting failed", e);
        }
    }
    
    /**
     * Encrypt PIN Block using Triple DES
     */
    public String encryptPINBlock(String pinBlock, String encryptionKey) {
        try {
            byte[] pinBlockBytes = hexStringToByteArray(pinBlock);
            byte[] keyBytes = hexStringToByteArray(encryptionKey);
            
            // Use DES (in production, use Triple DES)
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encrypted = cipher.doFinal(pinBlockBytes);
            String encryptedHex = byteArrayToHexString(encrypted);
            
            log.debug("PIN block encrypted");
            return encryptedHex;
            
        } catch (Exception e) {
            log.error("Error encrypting PIN block", e);
            throw new RuntimeException("PIN encryption failed", e);
        }
    }
    
    /**
     * Decrypt PIN Block
     */
    public String decryptPINBlock(String encryptedBlock, String decryptionKey) {
        try {
            byte[] encryptedBytes = hexStringToByteArray(encryptedBlock);
            byte[] keyBytes = hexStringToByteArray(decryptionKey);
            
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "DES");
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            return byteArrayToHexString(decrypted);
            
        } catch (Exception e) {
            log.error("Error decrypting PIN block", e);
            throw new RuntimeException("PIN decryption failed", e);
        }
    }
    
    /**
     * Verify PIN
     */
    public boolean verifyPIN(String encryptedPINBlock, String decryptionKey, 
                            String expectedPIN, String pan) {
        try {
            // Decrypt PIN block
            String decryptedBlock = decryptPINBlock(encryptedPINBlock, decryptionKey);
            
            // Extract PIN from block
            String extractedPIN = extractPINFromBlock(decryptedBlock, pan);
            
            // Compare
            boolean matches = expectedPIN.equals(extractedPIN);
            log.debug("PIN verification: {}", matches ? "SUCCESS" : "FAILED");
            
            return matches;
            
        } catch (Exception e) {
            log.error("Error verifying PIN", e);
            return false;
        }
    }
    
    private String extractPINFromBlock(String pinBlock, String pan) {
        // Format PAN block
        String panPart = pan.substring(pan.length() - 13, pan.length() - 1);
        String panBlock = "0000" + panPart;
        
        // XOR to get clear PIN block
        String clearPINBlock = xorHexStrings(pinBlock, panBlock);
        
        // Extract PIN length and PIN
        int pinLength = Character.getNumericValue(clearPINBlock.charAt(1));
        String pin = clearPINBlock.substring(2, 2 + pinLength);
        
        return pin;
    }
    
    // Utility methods
    private String xorHexStrings(String hex1, String hex2) {
        byte[] bytes1 = hexStringToByteArray(hex1);
        byte[] bytes2 = hexStringToByteArray(hex2);
        byte[] result = new byte[bytes1.length];
        
        for (int i = 0; i < bytes1.length; i++) {
            result[i] = (byte) (bytes1[i] ^ bytes2[i]);
        }
        
        return byteArrayToHexString(result);
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