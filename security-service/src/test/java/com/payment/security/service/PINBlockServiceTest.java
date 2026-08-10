package com.payment.security.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PIN blocks are how a cardholder's PIN travels through the network without
 * ever appearing in the clear: it's formatted (ISO 9564-1 Format 0), XOR'd
 * with the PAN, then encrypted. This is the same pipeline POSTransactionService
 * runs before it puts field 52 on the wire.
 */
class PINBlockServiceTest {

    private final PINBlockService pinBlockService = new PINBlockService();

    private static final String KEY = "0123456789ABCDEF";
    private static final String PAN = "4111111111111111";

    @Test
    void formatPINBlock_producesA16CharacterHexBlock() {
        String block = pinBlockService.formatPINBlock("1234", PAN);

        assertThat(block).hasSize(16).matches("[0-9A-Fa-f]+");
    }

    @Test
    void encryptThenDecrypt_roundTripsToTheSamePlainBlock() {
        String plainBlock = pinBlockService.formatPINBlock("1234", PAN);

        String encrypted = pinBlockService.encryptPINBlock(plainBlock, KEY);
        String decrypted = pinBlockService.decryptPINBlock(encrypted, KEY);

        assertThat(decrypted).isEqualToIgnoringCase(plainBlock);
        assertThat(encrypted).isNotEqualToIgnoringCase(plainBlock);
    }

    @Test
    void verifyPIN_succeedsForTheCorrectPIN() {
        String plainBlock = pinBlockService.formatPINBlock("1234", PAN);
        String encrypted = pinBlockService.encryptPINBlock(plainBlock, KEY);

        assertThat(pinBlockService.verifyPIN(encrypted, KEY, "1234", PAN)).isTrue();
    }

    @Test
    void verifyPIN_failsForTheWrongPIN() {
        String plainBlock = pinBlockService.formatPINBlock("1234", PAN);
        String encrypted = pinBlockService.encryptPINBlock(plainBlock, KEY);

        assertThat(pinBlockService.verifyPIN(encrypted, KEY, "9999", PAN)).isFalse();
    }

    @Test
    void verifyPIN_failsWhenDecryptedAgainstTheWrongPAN() {
        String plainBlock = pinBlockService.formatPINBlock("1234", PAN);
        String encrypted = pinBlockService.encryptPINBlock(plainBlock, KEY);

        assertThat(pinBlockService.verifyPIN(encrypted, KEY, "1234", "5500000000000004")).isFalse();
    }
}
