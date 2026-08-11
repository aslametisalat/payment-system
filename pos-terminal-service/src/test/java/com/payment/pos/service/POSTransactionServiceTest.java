package com.payment.pos.service;

import com.payment.common.enums.CardReadMethod;
import com.payment.common.enums.TransactionStatus;
import com.payment.iso8583.service.ISO8583MessageBuilder;
import com.payment.pos.client.TransactionClient;
import com.payment.pos.dto.TransactionRequest;
import com.payment.pos.dto.TransactionResponse;
import com.payment.pos.model.POSTransactionRequest;
import com.payment.pos.model.POSTransactionResult;
import com.payment.security.service.EMVCryptogramService;
import com.payment.security.service.MACService;
import com.payment.security.service.PINBlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This is the terminal's half of the "real-time" authorization flow from the
 * diagram: card read -> PIN block -> EMV cryptogram -> ISO 8583 message
 * build -> MAC. Every crypto/ISO8583 collaborator here is the real
 * production class (no mocks) - only java.util.Random is seeded, so
 * STAN/RRN are reproducible, and TransactionClient is mocked, standing in
 * for the real HTTP hop to transaction-service (which in turn calls
 * merchant/acquirer/network/issuer-service - see
 * TransactionProcessingServiceTest for that side of the chain). Together
 * the two test classes cover every hop without needing any service running.
 */
class POSTransactionServiceTest {

    private TransactionClient transactionClient;

    @BeforeEach
    void setUp() {
        transactionClient = mock(TransactionClient.class);
    }

    private POSTransactionService newService(Random random) {
        return new POSTransactionService(
                new ISO8583MessageBuilder(),
                new PINBlockService(),
                new MACService(),
                new EMVCryptogramService(),
                new CardReaderService(),
                new ReceiptService(),
                transactionClient,
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

    private TransactionResponse authorizedResponse() {
        TransactionResponse response = new TransactionResponse();
        response.setId("txn-1");
        response.setStatus(TransactionStatus.AUTHORIZED);
        response.setResponseCode("00");
        response.setAuthorizationCode("654321");
        return response;
    }

    private TransactionResponse declinedResponse(String responseCode, String responseMessage) {
        TransactionResponse response = new TransactionResponse();
        response.setId("txn-2");
        response.setStatus(TransactionStatus.DECLINED);
        response.setResponseCode(responseCode);
        response.setResponseMessage(responseMessage);
        return response;
    }

    @Test
    void processTransaction_approvesAChipAndPinPurchaseEndToEnd() {
        when(transactionClient.authorize(any())).thenReturn(authorizedResponse());
        POSTransactionService service = newService(new Random(42));

        POSTransactionResult result = service.processTransaction(sampleRequest()
                .cardReadMethod(CardReadMethod.CHIP)
                .requirePIN(true)
                .pin("1234")
                .build());

        assertThat(result.getApproved()).isTrue();
        assertThat(result.getResponseCode()).isEqualTo("00");
        assertThat(result.getAuthorizationCode()).isEqualTo("654321");
        assertThat(result.getTransactionId()).isEqualTo("txn-1");
        assertThat(result.getMacVerified()).isTrue();
        assertThat(result.getPinVerified()).isTrue();
        assertThat(result.getEmvVerified()).isTrue();
        assertThat(result.getStan()).matches("\\d{6}");
        assertThat(result.getRrn()).matches("\\d{12}");
        assertThat(result.getReceipt()).contains("APPROVED", "Coffee Shop");
    }

    @Test
    void processTransaction_sendsTheCardAmountAndMerchantThroughToTransactionService() {
        when(transactionClient.authorize(any())).thenReturn(authorizedResponse());
        POSTransactionService service = newService(new Random(1));

        service.processTransaction(sampleRequest()
                .cardReadMethod(CardReadMethod.CHIP)
                .requirePIN(true)
                .pin("1234")
                .build());

        ArgumentCaptor<TransactionRequest> captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionClient).authorize(captor.capture());
        TransactionRequest sent = captor.getValue();
        assertThat(sent.getMerchantId()).isEqualTo("merchant-1");
        assertThat(sent.getCardNumber()).isEqualTo("4111111111111111");
        assertThat(sent.getAmount()).isEqualByComparingTo("25.99"); // cents -> dollars
        assertThat(sent.getCurrency()).isEqualTo("USD");
    }

    @Test
    void processTransaction_reflectsADeclineFromTransactionServiceInTheReceipt() {
        // "03" maps to the generic ISO 8583 description "Invalid merchant",
        // but transaction-service's actual reason ("Daily limit exceeded")
        // must win - that's the message a cashier/cardholder actually needs.
        when(transactionClient.authorize(any()))
                .thenReturn(declinedResponse("03", "Daily limit exceeded"));
        POSTransactionService service = newService(new Random(3));

        POSTransactionResult result = service.processTransaction(sampleRequest()
                .cardReadMethod(CardReadMethod.CHIP)
                .requirePIN(true)
                .pin("1234")
                .build());

        assertThat(result.getApproved()).isFalse();
        assertThat(result.getResponseCode()).isEqualTo("03");
        assertThat(result.getResponseMessage()).isEqualTo("Daily limit exceeded");
        assertThat(result.getReceipt()).contains("DECLINED", "Daily limit exceeded");
    }

    @Test
    void processTransaction_fallsBackToTheGenericCodeDescriptionWhenNoSpecificMessageComesBack() {
        when(transactionClient.authorize(any())).thenReturn(declinedResponse("51", null));
        POSTransactionService service = newService(new Random(4));

        POSTransactionResult result = service.processTransaction(sampleRequest()
                .cardReadMethod(CardReadMethod.CHIP)
                .requirePIN(true)
                .pin("1234")
                .build());

        assertThat(result.getResponseMessage()).isEqualTo("Insufficient funds"); // ResponseCode enum's mapping for "51"
    }

    @Test
    void processTransaction_handlesMagneticStripeWithoutAPin() {
        when(transactionClient.authorize(any())).thenReturn(authorizedResponse());
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
    void processTransaction_returnsAnErrorResultWhenTransactionServiceIsUnreachable() {
        when(transactionClient.authorize(any())).thenThrow(new RuntimeException("transaction-service unreachable"));
        POSTransactionService service = newService(new Random(5));

        POSTransactionResult result = service.processTransaction(sampleRequest()
                .cardReadMethod(CardReadMethod.CHIP)
                .requirePIN(true)
                .pin("1234")
                .build());

        assertThat(result.getApproved()).isFalse();
        assertThat(result.getErrorMessage()).contains("transaction-service unreachable");
    }

    @Test
    void processTransaction_generatesADifferentStanAndRrnPerSeed() {
        when(transactionClient.authorize(any())).thenReturn(authorizedResponse());
        POSTransactionRequest request = sampleRequest()
                .cardReadMethod(CardReadMethod.CHIP)
                .requirePIN(true)
                .pin("1234")
                .build();

        POSTransactionResult first = newService(new Random(99)).processTransaction(request);
        POSTransactionResult second = newService(new Random(99)).processTransaction(request);
        POSTransactionResult third = newService(new Random(100)).processTransaction(request);

        assertThat(first.getStan()).isEqualTo(second.getStan());
        assertThat(first.getRrn()).isEqualTo(second.getRrn());
        assertThat(first.getStan()).isNotEqualTo(third.getStan());
    }
}
