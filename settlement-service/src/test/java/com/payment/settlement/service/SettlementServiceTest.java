package com.payment.settlement.service;

import com.payment.common.enums.SettlementStatus;
import com.payment.settlement.model.Settlement;
import com.payment.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Settlement is the last step of the flow diagram: once a transaction is
 * AUTHORIZED, the acquirer eventually pays the merchant out, minus its fee.
 */
class SettlementServiceTest {

    private SettlementRepository repository;
    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        repository = mock(SettlementRepository.class);
        settlementService = new SettlementService(repository);
        when(repository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createSettlement_deductsTheTwoPercentFeeFromTheNetAmount() {
        Settlement settlement = settlementService.createSettlement("merchant-1", new BigDecimal("1000.00"), 5);

        assertThat(settlement.getFees()).isEqualByComparingTo("20.0000"); // 2% of 1000
        assertThat(settlement.getNetAmount()).isEqualByComparingTo("980.0000");
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.getTransactionCount()).isEqualTo(5);
    }

    @Test
    void createSettlement_persistsTheSettlement() {
        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);

        settlementService.createSettlement("merchant-1", new BigDecimal("500.00"), 2);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMerchantId()).isEqualTo("merchant-1");
    }

    @Test
    void getMerchantSettlements_delegatesToTheRepository() {
        Settlement settlement = new Settlement();
        settlement.setMerchantId("merchant-1");
        when(repository.findByMerchantId("merchant-1")).thenReturn(List.of(settlement));

        List<Settlement> result = settlementService.getMerchantSettlements("merchant-1");

        assertThat(result).containsExactly(settlement);
    }
}
