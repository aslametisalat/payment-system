package com.payment.common.enums;

import lombok.Getter;

@Getter
public enum POSEntryMode {
    MANUAL("01", "Manual key entry"),
    MAGNETIC_STRIPE("02", "Magnetic stripe"),
    BARCODE("03", "Barcode"),
    OCR("04", "OCR"),
    CHIP("05", "Integrated circuit card (ICC)"),
    CONTACTLESS("07", "Contactless EMV"),
    MAGNETIC_STRIPE_FALLBACK("80", "Fallback from chip to magnetic stripe"),
    ECOMMERCE("81", "E-commerce transaction"),
    AUTO_ENTRY("90", "Full magnetic stripe read, transmit track data"),
    CREDENTIAL_ON_FILE("95", "Credential on file");
    
    private final String code;
    private final String description;
    
    POSEntryMode(String code, String description) {
        this.code = code;
        this.description = description;
    }
}