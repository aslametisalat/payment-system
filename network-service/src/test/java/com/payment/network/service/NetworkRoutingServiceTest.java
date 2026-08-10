package com.payment.network.service;

import com.payment.common.enums.PaymentNetwork;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The network is the middleman that reads the card's BIN (first 6 digits),
 * figures out which network/issuer owns it, and takes its own cut before
 * forwarding the request onward - the "→ NETWORK SERVICE → ISSUER SERVICE"
 * hop in the flow diagram. Deterministic (no randomness), so it's testable
 * as-is.
 */
class NetworkRoutingServiceTest {

    private final NetworkRoutingService routingService = new NetworkRoutingService();

    @Test
    void route_recognizesVisaByLeadingDigit4() {
        NetworkRoutingService.RoutingResult result =
                routingService.route("4111111111111111", new BigDecimal("1000"));

        assertThat(result.getNetwork()).isEqualTo(PaymentNetwork.VISA);
        assertThat(result.getNetworkFee()).isEqualByComparingTo("1.5000"); // 0.15%
        assertThat(result.getIssuerId()).isEqualTo("ISSUER-411111");
    }

    @Test
    void route_recognizesMastercardByLeadingDigit5() {
        NetworkRoutingService.RoutingResult result =
                routingService.route("5500000000000004", new BigDecimal("1000"));

        assertThat(result.getNetwork()).isEqualTo(PaymentNetwork.MASTERCARD);
        assertThat(result.getNetworkFee()).isEqualByComparingTo("1.4000"); // 0.14%
    }

    @Test
    void route_recognizesAmexByLeadingDigit3() {
        NetworkRoutingService.RoutingResult result =
                routingService.route("340000000000009", new BigDecimal("1000"));

        assertThat(result.getNetwork()).isEqualTo(PaymentNetwork.AMEX);
        assertThat(result.getNetworkFee()).isEqualByComparingTo("2.5000"); // 0.25%
    }

    @Test
    void route_fallsBackToLocalForUnrecognizedBins() {
        NetworkRoutingService.RoutingResult result =
                routingService.route("6011000000000004", new BigDecimal("1000"));

        assertThat(result.getNetwork()).isEqualTo(PaymentNetwork.LOCAL);
        assertThat(result.getNetworkFee()).isEqualByComparingTo("1.0000"); // 0.10%
    }

    @Test
    void route_derivesTheIssuerIdFromTheFullBin() {
        NetworkRoutingService.RoutingResult result =
                routingService.route("400000123456789", new BigDecimal("10"));

        assertThat(result.getIssuerId()).isEqualTo("ISSUER-400000");
    }
}
