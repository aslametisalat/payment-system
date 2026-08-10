package com.payment.transaction.service;

import com.payment.common.enums.TransactionStatus;
import com.payment.common.enums.TransactionType;
import com.payment.transaction.client.IssuerClient;
import com.payment.transaction.dto.AuthorizationResponse;
import com.payment.transaction.dto.TransactionRequest;
import com.payment.transaction.dto.TransactionResponse;
import com.payment.transaction.model.Transaction;
import com.payment.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TransactionProcessingService is the orchestrator at the center of the flow
 * diagram: it records the attempt, delegates the approve/decline decision to
 * the issuer (over Feign, mocked here), and persists the outcome. Because it
 * only depends on an interface (IssuerClient) and a Spring Data repository -
 * both trivially mockable - this class needed no refactor to become testable.
 */
class TransactionProcessingServiceTest {

    private TransactionRepository transactionRepository;
    private IssuerClient issuerClient;
    private TransactionProcessingService service;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        issuerClient = mock(IssuerClient.class);
        service = new TransactionProcessingService(transactionRepository, issuerClient);

        // Simulate JPA assigning an ID and stamping defaults on first save.
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId("txn-1");
            }
            return t;
        });
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
    void processTransaction_marksTheTransactionAuthorizedWhenTheIssuerApproves() {
        when(issuerClient.authorize(any())).thenReturn(approvedResponse());

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // PENDING, then final state
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
    void processTransaction_marksTheTransactionFailedWhenTheIssuerCallErrorsOut() {
        when(issuerClient.authorize(any())).thenThrow(new RuntimeException("issuer-service unreachable"));

        TransactionResponse response = service.processTransaction(sampleRequest());

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getResponseMessage()).contains("System error");
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
