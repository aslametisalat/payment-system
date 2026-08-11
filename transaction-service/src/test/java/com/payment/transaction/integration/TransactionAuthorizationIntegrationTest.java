package com.payment.transaction.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.common.enums.PaymentNetwork;
import com.payment.transaction.client.AcquirerClient;
import com.payment.transaction.client.IssuerClient;
import com.payment.transaction.client.MerchantClient;
import com.payment.transaction.client.NetworkClient;
import com.payment.transaction.dto.AcquirerResponse;
import com.payment.transaction.dto.AuthorizationResponse;
import com.payment.transaction.dto.MerchantValidationResult;
import com.payment.transaction.dto.RoutingResponse;
import com.payment.transaction.dto.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the full authorization chain from the flow diagram:
 *
 *   HTTP POST /api/transactions/authorize
 *     -> TransactionController
 *     -> TransactionProcessingService (real bean)
 *     -> TransactionRepository (real H2 database, created fresh per run)
 *     -> MerchantClient -> AcquirerClient -> NetworkClient -> IssuerClient
 *        (all four mocked - stand in for merchant-service, acquirer-service,
 *        network-service and issuer-service, none of which are running
 *        during this test)
 *
 * This is the closest thing to "watching a transaction happen in real time"
 * without needing all thirteen services and Eureka/Config Server up: it
 * drives a real HTTP request through the real controller/service/repository
 * stack and only replaces the network calls that would otherwise leave
 * the JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TransactionAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MerchantClient merchantClient;

    @MockBean
    private AcquirerClient acquirerClient;

    @MockBean
    private NetworkClient networkClient;

    @MockBean
    private IssuerClient issuerClient;

    @BeforeEach
    void stubTheUpstreamHopsAsHealthy() {
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

    private TransactionRequest purchaseRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setMerchantId("merchant-1");
        request.setCardNumber("4111111111111111");
        request.setCvv("123");
        request.setTerminalId("TERM0001");
        request.setType(com.payment.common.enums.TransactionType.PURCHASE);
        request.setAmount(new BigDecimal("42.50"));
        request.setCurrency("USD");
        return request;
    }

    @Test
    void authorize_persistsAndReturnsAnAuthorizedTransactionWhenEveryHopApproves() throws Exception {
        AuthorizationResponse approved = new AuthorizationResponse();
        approved.setApproved(true);
        approved.setAuthorizationCode("654321");
        approved.setResponseCode("00");
        approved.setMessage("Approved");
        when(issuerClient.authorize(any())).thenReturn(approved);

        String responseBody = mockMvc.perform(post("/api/transactions/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.authorizationCode").value("654321"))
                .andReturn().getResponse().getContentAsString();

        String transactionId = objectMapper.readTree(responseBody).get("id").asText();

        // The authorized transaction really landed in the database, not just
        // in the HTTP response - fetch it back independently.
        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        mockMvc.perform(get("/api/transactions/merchant/{merchantId}", "merchant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantId").value("merchant-1"));
    }

    @Test
    void authorize_returnsOkWithDeclinedStatusWhenTheMerchantIsRejected() throws Exception {
        // A decline is a legitimate business outcome for a well-formed
        // request, not a client error - it comes back as 200 so callers
        // like the POS terminal (a real Feign client) can read the decline
        // reason instead of Feign throwing on a non-2xx response.
        MerchantValidationResult invalid = new MerchantValidationResult();
        invalid.setValid(false);
        invalid.setMessage("Daily limit exceeded");
        when(merchantClient.validateForTransaction(any(), any())).thenReturn(invalid);

        mockMvc.perform(post("/api/transactions/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.responseMessage").value("Daily limit exceeded"));
    }

    @Test
    void authorize_returnsOkWithDeclinedStatusWhenTheIssuerDeclines() throws Exception {
        AuthorizationResponse declined = new AuthorizationResponse();
        declined.setApproved(false);
        declined.setResponseCode("51");
        declined.setMessage("Insufficient funds");
        when(issuerClient.authorize(any())).thenReturn(declined);

        mockMvc.perform(post("/api/transactions/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchaseRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.responseMessage").value("Insufficient funds"));
    }

    @Test
    void authorize_rejectsARequestMissingRequiredFields() throws Exception {
        TransactionRequest incomplete = new TransactionRequest();
        incomplete.setAmount(new BigDecimal("10.00")); // missing merchantId, cardNumber, cvv, type

        mockMvc.perform(post("/api/transactions/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomplete)))
                .andExpect(status().isBadRequest());
    }
}
