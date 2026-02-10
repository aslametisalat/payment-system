package com.payment.iso8583.model;

import lombok.Data;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

@Data
public class ISO8583Message {
    private String mti; // Message Type Indicator
    private BitSet bitmap;
    private Map<Integer, String> dataElements;
    
    public ISO8583Message() {
        this.bitmap = new BitSet(128);
        this.dataElements = new HashMap<>();
    }
    
    public void setField(int fieldNumber, String value) {
        if (fieldNumber < 2 || fieldNumber > 128) {
            throw new IllegalArgumentException("Field number must be between 2 and 128");
        }
        dataElements.put(fieldNumber, value);
        bitmap.set(fieldNumber - 1);
    }
    
    public String getField(int fieldNumber) {
        return dataElements.get(fieldNumber);
    }
    
    public boolean hasField(int fieldNumber) {
        return bitmap.get(fieldNumber - 1);
    }
    
    public String toBitmapHex() {
        StringBuilder hex = new StringBuilder();
        
        // Convert BitSet to hex (16 bytes = 128 bits)
        for (int i = 0; i < 128; i += 8) {
            int byteValue = 0;
            for (int j = 0; j < 8; j++) {
                if (bitmap.get(i + j)) {
                    byteValue |= (1 << (7 - j));
                }
            }
            hex.append(String.format("%02X", byteValue));
        }
        
        return hex.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MTI: ").append(mti).append("\n");
        sb.append("Bitmap: ").append(toBitmapHex()).append("\n");
        sb.append("Fields:\n");
        
        for (Map.Entry<Integer, String> entry : dataElements.entrySet()) {
            sb.append(String.format("  Field %03d: %s\n", entry.getKey(), entry.getValue()));
        }
        
        return sb.toString();
    }
}