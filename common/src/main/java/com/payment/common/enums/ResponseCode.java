package com.payment.common.enums;

import lombok.Getter;

@Getter
public enum ResponseCode {
    APPROVED("00", "Approved"),
    REFER_TO_ISSUER("01", "Refer to card issuer"),
    REFER_TO_ISSUER_SPECIAL("02", "Refer to card issuer, special condition"),
    INVALID_MERCHANT("03", "Invalid merchant"),
    PICK_UP_CARD("04", "Pick up card - fraud"),
    DO_NOT_HONOR("05", "Do not honor"),
    ERROR("06", "Error"),
    PICK_UP_CARD_SPECIAL("07", "Pick up card, special condition"),
    INVALID_TRANSACTION("12", "Invalid transaction"),
    INVALID_AMOUNT("13", "Invalid amount"),
    INVALID_CARD_NUMBER("14", "Invalid card number"),
    NO_SUCH_ISSUER("15", "No such issuer"),
    FORMAT_ERROR("30", "Format error"),
    LOST_CARD("41", "Lost card, pick up"),
    STOLEN_CARD("43", "Stolen card, pick up"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    EXPIRED_CARD("54", "Expired card"),
    INCORRECT_PIN("55", "Incorrect PIN"),
    TRANSACTION_NOT_PERMITTED_CARDHOLDER("57", "Transaction not permitted to cardholder"),
    TRANSACTION_NOT_PERMITTED_TERMINAL("58", "Transaction not permitted to terminal"),
    EXCEEDS_WITHDRAWAL_LIMIT("61", "Exceeds withdrawal amount limit"),
    RESTRICTED_CARD("62", "Restricted card"),
    SECURITY_VIOLATION("63", "Security violation"),
    EXCEEDS_FREQUENCY_LIMIT("65", "Exceeds withdrawal frequency limit"),
    PIN_TRIES_EXCEEDED("75", "Allowable PIN tries exceeded"),
    SYSTEM_MALFUNCTION("96", "System malfunction");
    
    private final String code;
    private final String message;
    
    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public static ResponseCode fromCode(String code) {
        for (ResponseCode rc : values()) {
            if (rc.code.equals(code)) {
                return rc;
            }
        }
        return SYSTEM_MALFUNCTION;
    }
}