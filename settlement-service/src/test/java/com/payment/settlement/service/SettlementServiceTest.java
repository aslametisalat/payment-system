package com.payment.settlement.service;

import com.payment.common.enums.SettlementStatus;
import com.payment.settlement.client.MerchantClient;
import com.payment.settlement.client.TransactionClient;
import com.payment.settlement.dto.MerchantSummary;
import com.payment.settlement.dto.SettlementRequest;
import com.payment.settlement.dto.SettlementResponse;
import com.payment.settlement.dto.TransactionSummary;
import com.payment.settlement.model.Settlement;
import com.payment.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Settlement is the last step of the flow diagram: once transactions are
 * AUTHORIZED, the acquirer pays the merchant out, minus its fee. This
 * service pulls the merchant's transactions from transaction-service and
 * the merchant list from merchant-service over Feign (both mocked here),
 * so the money math can be tested without either service running.
 */
class SettlementServiceTest {

    private SettlementRepository repository;
    private TransactionClient transactionClient;
    private MerchantClient merchantClient;
    private SettlementService settlementService;

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @BeforeEach
    void setUp() {
        repository = mock(SettlementRepository.class);
        transactionClient = mock(TransactionClient.class);
        merchantClient = mock(MerchantClient.class);
        settlementService = new SettlementService(repository, transactionClient, merchantClient);
        when(repository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private TransactionSummary transaction(String status, String amount, LocalDate date) {
        TransactionSummary t = new TransactionSummary();
        t.setStatus(status);
        t.setAmount(new BigDecimal(amount));
        t.setCreatedAt(date.atTime(12, 0));
        return t;
    }

    @Test
    void createSettlement_sumsOnlyApprovedTransactionsFromTheSettlementDate() {
        when(repository.existsByMerchantIdAndSettlementDate("merchant-1", TODAY)).thenReturn(false);
        when(transactionClient.getMerchantTransactions("merchant-1")).thenReturn(List.of(
                transaction("AUTHORIZED", "100.00", TODAY),
                transaction("AUTHORIZED", "50.00", TODAY),
                transaction("DECLINED", "999.00", TODAY),       // wrong status - excluded
                transaction("AUTHORIZED", "25.00", TODAY.minusDays(1)) // wrong date - excluded
        ));

        SettlementResponse response = settlementService.createSettlement(
                SettlementRequest.builder().merchantId("merchant-1").settlementDate(TODAY).build());

        assertThat(response.getTransactionCount()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(response.getFees()).isEqualByComparingTo("3.0000"); // 2% of 150
        assertThat(response.getNetAmount()).isEqualByComparingTo("147.0000");
        assertThat(response.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    void createSettlement_defaultsTheSettlementDateToToday() {
        when(transactionClient.getMerchantTransactions("merchant-1")).thenReturn(List.of());

        settlementService.createSettlement(SettlementRequest.builder().merchantId("merchant-1").build());

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSettlementDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void createSettlement_rejectsADuplicateSettlementForTheSameMerchantAndDate() {
        when(repository.existsByMerchantIdAndSettlementDate("merchant-1", TODAY)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> settlementService.createSettlement(
                SettlementRequest.builder().merchantId("merchant-1").settlementDate(TODAY).build()));

        verify(repository, never()).save(any());
    }

    @Test
    void createSettlement_treatsAnUnreachableTransactionServiceAsZeroTransactions() {
        when(transactionClient.getMerchantTransactions("merchant-1"))
                .thenThrow(new RuntimeException("transaction-service unreachable"));

        SettlementResponse response = settlementService.createSettlement(
                SettlementRequest.builder().merchantId("merchant-1").settlementDate(TODAY).build());

        assertThat(response.getTransactionCount()).isEqualTo(0);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getMerchantSettlements_delegatesToTheRepository() {
        Settlement settlement = new Settlement();
        settlement.setMerchantId("merchant-1");
        settlement.setTotalAmount(BigDecimal.TEN);
        settlement.setFees(BigDecimal.ZERO);
        settlement.setNetAmount(BigDecimal.TEN);
        when(repository.findByMerchantId("merchant-1")).thenReturn(List.of(settlement));

        List<SettlementResponse> result = settlementService.getMerchantSettlements("merchant-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMerchantId()).isEqualTo("merchant-1");
    }

    @Test
    void processSettlements_skipsMerchantsAlreadySettledToday() {
        MerchantSummary merchant = new MerchantSummary();
        merchant.setId("merchant-1");
        when(merchantClient.getAllMerchants()).thenReturn(List.of(merchant));
        when(repository.existsByMerchantIdAndSettlementDate(eq("merchant-1"), any(LocalDate.class)))
                .thenReturn(true);

        settlementService.processSettlements();

        verifyNoInteractions(transactionClient);
        verify(repository, never()).save(any());
    }

    @Test
    void processSettlements_doesNotPropagateWhenMerchantServiceIsUnreachable() {
        when(merchantClient.getAllMerchants()).thenThrow(new RuntimeException("merchant-service unreachable"));

        settlementService.processSettlements(); // should not throw

        verifyNoInteractions(transactionClient);
    }
}
