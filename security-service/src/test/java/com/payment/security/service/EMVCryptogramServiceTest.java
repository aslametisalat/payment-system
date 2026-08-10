package com.payment.security.service;

import com.payment.common.dto.EMVTransactionData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * EMV cryptograms are how a chip card proves to the issuer that a specific
 * transaction was authorized by the genuine card (ARQC), and how the issuer
 * proves its response is genuine back to the card (ARPC). This is the
 * "→ Step 3: Generating EMV cryptogram..." step in POSTransactionService.
 */
class EMVCryptogramServiceTest {

    private final EMVCryptogramService emvService = new EMVCryptogramService();

    private static final String CARD_MASTER_KEY = "0123456789ABCDEF";

    private EMVTransactionData sampleTransaction() {
        return EMVTransactionData.builder()
                .amount(10000L)
                .currencyCode(840)
                .transactionDate("260810")
                .transactionType(0)
                .unpredictableNumber("12345678")
                .atc(1)
                .build();
    }

    @Test
    void generateARQC_isDeterministicForTheSameTransactionAndKey() {
        EMVTransactionData txn = sampleTransaction();

        String arqc1 = emvService.generateARQC(txn, CARD_MASTER_KEY);
        String arqc2 = emvService.generateARQC(txn, CARD_MASTER_KEY);

        assertThat(arqc1).isEqualTo(arqc2).hasSize(16).matches("[0-9A-F]+");
    }

    @Test
    void generateARQC_changesWhenTheTransactionCounterChanges() {
        EMVTransactionData first = sampleTransaction();
        EMVTransactionData second = EMVTransactionData.builder()
                .amount(10000L)
                .currencyCode(840)
                .transactionDate("260810")
                .transactionType(0)
                .unpredictableNumber("12345678")
                .atc(2)
                .build();

        String arqcFirst = emvService.generateARQC(first, CARD_MASTER_KEY);
        String arqcSecond = emvService.generateARQC(second, CARD_MASTER_KEY);

        assertThat(arqcFirst).isNotEqualTo(arqcSecond);
    }

    @Test
    void generateARPC_succeedsWhenTheARQCMatchesTheTransaction() {
        EMVTransactionData txn = sampleTransaction();
        String arqc = emvService.generateARQC(txn, CARD_MASTER_KEY);

        String arpc = emvService.generateARPC(arqc, txn, CARD_MASTER_KEY, "654321");

        assertThat(arpc).hasSize(16).matches("[0-9A-F]+");
    }

    @Test
    void generateARPC_rejectsAnARQCThatDoesNotMatchTheTransaction() {
        EMVTransactionData txn = sampleTransaction();

        assertThrows(RuntimeException.class, () ->
                emvService.generateARPC("0000000000000000", txn, CARD_MASTER_KEY, "654321"));
    }
}
