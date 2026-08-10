package com.payment.pos.service;

import com.payment.common.enums.CardReadMethod;
import com.payment.iso8583.service.ISO8583MessageBuilder;
import com.payment.pos.model.POSTransactionRequest;
import com.payment.pos.model.POSTransactionResult;
import com.payment.security.service.EMVCryptogramService;
import com.payment.security.service.MACService;
import com.payment.security.service.PINBlockService;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the whole "real-time" authorization flow from the diagram, run
 * locally in a single test: card read -> PIN block -> EMV cryptogram ->
 * ISO 8583 message build -> MAC -> simulated authorization -> response MAC
 * verification -> receipt. Every collaborator here is the real production
 * class (no mocks) - only java.util.Random is seeded, so STAN/RRN/auth
 * codes are reproducible instead of different on every run.
 *
 * Writing this test is what surfaced a real bug: sendToAuthorization() built
 * a simulated response but never set field 64 (the response MAC), so
 * verifyResponseMAC() always failed and every POS transaction was declined
 * as a "Security violation - Invalid MAC" regardless of the card or amount.
 * That's now fixed in POSTransactionService - see the comment in
 * sendToAuthorization().
 */
class POSTransactionServiceTest {

    private POSTransactionService newService(Random random) {
        return new POSTransactionService(
                new ISO8583MessageBuilder(),
                new PINBlockService(),
                new MACService(),
                new EMVCryptogramService(),
                new CardReaderService(),
                new ReceiptService(),
                random);
    }

    private POSTransactionRequest.POSTransactionRequestBuilder sampleRequest() {
        return POSTransactionRequest.builder()
                .terminalId("TERM0001")
                .merchantId("merchant-1")
                .merchantName("Coffee Shop")
                .amount(2599L) // $25.99
                .cardNumber("4111111111111111")
                .expiryDate("2812")
                .cvv("123");
    }

    @Test
    void processTransaction_approvesAChipAndPinPurchaseEndToEnd() {
        POSTransactionService service = newService(new Random(42));

        POSTransactionResult result = service.processTransaction(sampleRequest()
                .cardReadMethod(CardReadMethod.CHIP)
                .requirePIN(true)
                .pin("1234")
                .build());

        assertThat(result.getApproved()).isTrue();
        assertThat(result.getResponseCode()).isEqualTo("00");
        assertThat(result.getAuthorizationCode()).matches("\\d{6}");
        assertThat(result.getMacVerified()).isTrue();
        assertThat(result.getPinVerified()).isTrue();
        assertThat(result.getEmvVerified()).isTrue();
        assertThat(result.getStan()).matches("\\d{6}");
        assertThat(result.getRrn()).matches("\\d{12}");
        assertThat(result.getReceipt()).contains("APPROVED", "Coffee Shop");
    }

    @Test
    void processTransaction_handlesMagneticStripeWithoutAPin() {
        POSTransactionService service = newService(new Random(7));

        POSTransactionResult result = service.processTransaction(sampleRequest()
                .cardReadMethod(CardReadMethod.MAGNETIC_STRIPE)
                .requirePIN(false)
                .build());

        assertThat(result.getApproved()).isTrue();
        assertThat(result.getPinVerified()).isFalse();
        assertThat(result.getEmvVerified()).isFalse(); // EMV only runs for CHIP reads
        assertThat(result.getMacVerified()).isTrue();
    }

    @Test
    void processTransaction_isDeterministicForTheSameRandomSeed() {
        POSTransactionRequest request = sampleRequest()
                .cardReadMethod(CardReadMethod.CHIP)
                .requirePIN(true)
                .pin("1234")
                .build();

        POSTransactionResult first = newService(new Random(99)).processTransaction(request);
        POSTransactionResult second = newService(new Random(99)).processTransaction(request);

        assertThat(first.getStan()).isEqualTo(second.getStan());
        assertThat(first.getRrn()).isEqualTo(second.getRrn());
        assertThat(first.getAuthorizationCode()).isEqualTo(second.getAuthorizationCode());
    }
}
