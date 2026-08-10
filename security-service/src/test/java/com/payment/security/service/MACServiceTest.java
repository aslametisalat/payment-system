package com.payment.security.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MAC (Message Authentication Code) protects ISO 8583 messages in transit:
 * both ends compute a MAC over the message body with a shared key, so a
 * tampered message or a wrong key is detected before it's trusted.
 */
class MACServiceTest {

    private final MACService macService = new MACService();

    private static final String KEY = "FEDCBA9876543210";

    @Test
    void generateMAC_isDeterministicForSameInput() {
        String mac1 = macService.generateMAC("0100PAN4111111111111111AMT000000010000", KEY);
        String mac2 = macService.generateMAC("0100PAN4111111111111111AMT000000010000", KEY);

        assertThat(mac1).isEqualTo(mac2);
        assertThat(mac1).hasSize(16).matches("[0-9A-F]+");
    }

    @Test
    void generateMAC_differsWhenMessageChanges() {
        String mac1 = macService.generateMAC("message-one", KEY);
        String mac2 = macService.generateMAC("message-two", KEY);

        assertThat(mac1).isNotEqualTo(mac2);
    }

    @Test
    void verifyMAC_succeedsForMatchingMac() {
        String message = "0100002000000000000000123456";
        String mac = macService.generateMAC(message, KEY);

        assertThat(macService.verifyMAC(message, mac, KEY)).isTrue();
    }

    @Test
    void verifyMAC_isCaseInsensitive() {
        String message = "case-insensitive-check";
        String mac = macService.generateMAC(message, KEY);

        assertThat(macService.verifyMAC(message, mac.toLowerCase(), KEY)).isTrue();
    }

    @Test
    void verifyMAC_failsWhenMessageWasTamperedWith() {
        String original = "amount=100.00";
        String mac = macService.generateMAC(original, KEY);

        assertThat(macService.verifyMAC("amount=999.00", mac, KEY)).isFalse();
    }

    @Test
    void verifyMAC_failsWithWrongKey() {
        String message = "amount=100.00";
        String mac = macService.generateMAC(message, KEY);

        assertThat(macService.verifyMAC(message, mac, "0000000000000000")).isFalse();
    }
}
