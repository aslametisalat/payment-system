package com.payment.acquirer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The acquirer is the merchant's bank: before a transaction is even routed
 * to a card network, it runs fraud screening and velocity checks. This is
 * the "→ ACQUIRER SERVICE" hop right after Merchant Service in the flow.
 *
 * Math.random() was replaced with an injected java.util.Random so its
 * "random component for simulation" can be pinned down in tests instead of
 * making the fraud score - and therefore the approve/decline outcome -
 * different on every run.
 */
class AcquirerServiceTest {

    private Random random;
    private AcquirerService acquirerService;

    @BeforeEach
    void setUp() {
        random = mock(Random.class);
        acquirerService = new AcquirerService(random);
    }

    @Test
    void performFraudCheck_addsRiskForAHighValueTransaction() {
        when(random.nextInt(30)).thenReturn(0);

        int lowAmountScore = acquirerService.performFraudCheck("4111111111111111", new BigDecimal("50"));
        int highAmountScore = acquirerService.performFraudCheck("4111111111111111", new BigDecimal("5000"));

        assertThat(lowAmountScore).isEqualTo(0);
        assertThat(highAmountScore).isEqualTo(20); // > $1000 adds a flat 20 points
    }

    @Test
    void performFraudCheck_addsTheRandomSimulationComponent() {
        when(random.nextInt(30)).thenReturn(15);

        int score = acquirerService.performFraudCheck("4111111111111111", new BigDecimal("50"));

        assertThat(score).isEqualTo(15);
    }

    @Test
    void checkVelocity_currentlyAlwaysPasses() {
        // Documents current (simplified) behavior: in a real acquirer this
        // would look at recent transaction frequency for the card.
        assertThat(acquirerService.checkVelocity("4111111111111111")).isTrue();
    }

    @Test
    void processAcquiring_approvesATypicalTransaction() {
        when(random.nextInt(30)).thenReturn(10);

        AcquirerService.AcquiringResult result =
                acquirerService.processAcquiring("merchant-1", "4111111111111111", new BigDecimal("75.00"));

        assertThat(result.isApproved()).isTrue();
        assertThat(result.getMessage()).isEqualTo("Approved");
    }

    @Test
    void processAcquiring_staysApprovedEvenAtMaximumSimulatedRisk() {
        // Highest reachable score today is 20 (high amount) + 29 (max random
        // component) = 49, which never crosses the 80-point decline
        // threshold below. Worth knowing when using this as a learning
        // example: the decline branch in processAcquiring() is currently
        // unreachable through fraud scoring alone.
        when(random.nextInt(30)).thenReturn(29);

        AcquirerService.AcquiringResult result =
                acquirerService.processAcquiring("merchant-1", "4111111111111111", new BigDecimal("5000.00"));

        assertThat(result.isApproved()).isTrue();
    }
}
