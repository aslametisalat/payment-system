package com.payment.transaction.service;

import com.payment.common.enums.PaymentNetwork;
import com.payment.common.enums.TransactionStatus;
import com.payment.common.enums.TransactionType;
import com.payment.transaction.client.AcquirerClient;
import com.payment.transaction.client.IssuerClient;
import com.payment.transaction.client.MerchantClient;
import com.payment.transaction.client.NetworkClient;
import com.payment.transaction.dto.AcquirerResponse;
import com.payment.transaction.dto.AuthorizationResponse;
import com.payment.transaction.dto.MerchantValidationResult;
import com.payment.transaction.dto.RoutingResponse;
import com.payment.transaction.dto.TransactionRequest;
import com.payment.transaction.dto.TransactionResponse;
import com.payment.transaction.model.Transaction;
import com.payment.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TransactionProcessingService is the orchestrator at the center of the flow
 * diagram: merchant limits, acquirer fraud screening, network routing, and
 * finally the issuer's approve/decline call - four real hops, all over
 * Feign, all mocked here so the orchestration logic (who gets called, in
 * what order, and what short-circuits it) can be tested without any of the
 * other services running.
 */
class TransactionProcessingServiceTest {

    private TransactionRepository transactionRepository;
    private MerchantClient merchantClient;
    private AcquirerClient acquirerClient;
    private NetworkClient networkClient;
    private IssuerClient issuerClient;
    private TransactionProcessingService service;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        merchantClient = mock(MerchantClient.class);
        acquirerClient = mock(AcquirerClient.class);
        networkClient = mock(NetworkClient.class);
        issuerClient = mock(IssuerClient.class);
        service = new TransactionProcessingService(
                transactionRepository, merchantClient, acquirerClient, networkClient, issuerClient);

        // Simulate JPA assigning an ID and stamping defaults on first save.
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId("txn-1");
            }
            return t;
        });

        // Happy-path stubs for the three upstream hops; individual tests
        // override whichever one they care about.
        MerchantValidationResult validMerchant = new MerchantValidationResult();
        validMerchant.setValid(true);
        when(merchantClient.validateForTransaction(any(), any())).thenReturn(validMerchant);

        AcquirerResponse acquirerApproved = new AcquirerResponse();
        acquirerApproved.setApproved(true);
        acquirerApproved.setAcquirerId("ACQ-123456");
        when(acquirerClient.processAcquiring(any())).thenReturn(acquirerApproved);

        RoutingResponse routing = new RoutingResponse();
        routing.setNetwork(PaymentNetwork.VISA);
        routing.setIssuerId("ISSUER-411111");
        when(networkClient.route(any())).thenReturn(routing);
    }

    private TransactionRequest sampleRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setMerchantId("merchant-1");
        request.setCardNumber("4111111111111111");
        request.setCvv("123");
        request.setTerminalId("TERM0001");
        request.setType(TransactionType.PURCHASE);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        return request;
    }

    @Test
    void processTransaction_walksAllFourHopsWhenEveryoneApproves() {
        when(issuerClient.authorize(any())).thenReturn(approvedResponse());

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
        assertThat(response.getResponseCode()).isEqualTo("00");
        verify(merchantClient).validateForTransaction("merchant-1", new BigDecimal("100.00"));
        verify(acquirerClient).processAcquiring(any());
        verify(networkClient).route(any());
        verify(issuerClient).authorize(any());
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // PENDING, then final state
    }

    @Test
    void processTransaction_recordsTheAcquirerAndNetworkTrailOnTheTransaction() {
        when(issuerClient.authorize(any())).thenReturn(approvedResponse());

        service.processTransaction(sampleRequest());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, atLeastOnce()).save(captor.capture());
        Transaction finalSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalSave.getAcquirerId()).isEqualTo("ACQ-123456");
        assertThat(finalSave.getNetworkId()).isEqualTo("VISA");
        assertThat(finalSave.getIssuerId()).isEqualTo("ISSUER-411111");
    }

    @Test
    void processTransaction_declinesAtTheMerchantStageWithoutCallingAnyoneElse() {
        MerchantValidationResult invalid = new MerchantValidationResult();
        invalid.setValid(false);
        invalid.setMessage("Daily limit exceeded");
        when(merchantClient.validateForTransaction(any(), any())).thenReturn(invalid);

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getResponseMessage()).isEqualTo("Daily limit exceeded");
        verifyNoInteractions(acquirerClient, networkClient, issuerClient);
    }

    @Test
    void processTransaction_declinesAtTheAcquirerStageWithoutCallingNetworkOrIssuer() {
        AcquirerResponse declined = new AcquirerResponse();
        declined.setApproved(false);
        declined.setMessage("Suspected fraud");
        when(acquirerClient.processAcquiring(any())).thenReturn(declined);

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getResponseMessage()).isEqualTo("Suspected fraud");
        verifyNoInteractions(networkClient, issuerClient);
    }

    @Test
    void processTransaction_marksTheTransactionDeclinedWhenTheIssuerDeclines() {
        AuthorizationResponse declined = new AuthorizationResponse();
        declined.setApproved(false);
        declined.setResponseCode("51");
        declined.setMessage("Insufficient funds");
        when(issuerClient.authorize(any())).thenReturn(declined);

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(response.getResponseMessage()).isEqualTo("Insufficient funds");
    }

    @Test
    void processTransaction_marksTheTransactionFailedWhenAnyHopErrorsOut() {
        when(issuerClient.authorize(any())).thenThrow(new RuntimeException("issuer-service unreachable"));

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getResponseMessage()).contains("System error");
    }

    @Test
    void processTransaction_recordsAStepPerHopWhenEverythingSucceeds() {
        when(issuerClient.authorize(any())).thenReturn(approvedResponse());

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getSteps()).extracting("stepName", "target", "status").containsExactly(
                org.assertj.core.groups.Tuple.tuple("Merchant Validation", "merchant-service", "SUCCESS"),
                org.assertj.core.groups.Tuple.tuple("Acquirer Processing", "acquirer-service", "SUCCESS"),
                org.assertj.core.groups.Tuple.tuple("Network Routing", "network-service", "SUCCESS"),
                org.assertj.core.groups.Tuple.tuple("Issuer Authorization", "issuer-service", "SUCCESS"));
    }

    @Test
    void processTransaction_stepTraceShowsExactlyWhichHopDeclinedIt() {
        AcquirerResponse declined = new AcquirerResponse();
        declined.setApproved(false);
        declined.setMessage("Suspected fraud");
        when(acquirerClient.processAcquiring(any())).thenReturn(declined);

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getSteps()).extracting("stepName", "target", "status").containsExactly(
                org.assertj.core.groups.Tuple.tuple("Merchant Validation", "merchant-service", "SUCCESS"),
                org.assertj.core.groups.Tuple.tuple("Acquirer Processing", "acquirer-service", "SUCCESS"),
                org.assertj.core.groups.Tuple.tuple("Declined", "acquirer-service", "DECLINED"));
    }

    @Test
    void processTransaction_stepTraceShowsExactlyWhichHopFailed() {
        when(issuerClient.authorize(any())).thenThrow(new RuntimeException("issuer-service unreachable"));

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getSteps()).extracting("stepName", "target", "status").containsExactly(
                org.assertj.core.groups.Tuple.tuple("Merchant Validation", "merchant-service", "SUCCESS"),
                org.assertj.core.groups.Tuple.tuple("Acquirer Processing", "acquirer-service", "SUCCESS"),
                org.assertj.core.groups.Tuple.tuple("Network Routing", "network-service", "SUCCESS"),
                org.assertj.core.groups.Tuple.tuple("Issuer Authorization", "issuer-service", "FAILED"));
    }

    @Test
    void processTransaction_truncatesAnOversizedErrorMessageInsteadOfFailingTheSave() {
        // Feign exceptions embed the failing URL and response body in
        // getMessage() (real example: a 503 from a Feign call routinely
        // exceeds 255 chars) - response_message is a VARCHAR(255) column,
        // so without truncation the save itself throws.
        String hugeMessage = "x".repeat(400);
        when(issuerClient.authorize(any())).thenThrow(new RuntimeException(hugeMessage));

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getResponseMessage().length()).isLessThanOrEqualTo(255);
    }

    @Test
    void getTransaction_throwsWhenTheTransactionDoesNotExist() {
        when(transactionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getTransaction("missing"));
    }

    private AuthorizationResponse approvedResponse() {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setApproved(true);
        response.setAuthorizationCode("654321");
        response.setResponseCode("00");
        response.setMessage("Approved");
        return response;
    }
}
