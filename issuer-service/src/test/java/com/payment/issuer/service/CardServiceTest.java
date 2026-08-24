package com.payment.issuer.service;

import com.payment.issuer.dto.AuthorizationRequest;
import com.payment.issuer.dto.AuthorizationResponse;
import com.payment.issuer.dto.CardRequest;
import com.payment.issuer.dto.CardResponse;
import com.payment.issuer.model.Card;
import com.payment.issuer.repository.CardRepository;
import com.payment.security.service.EMVCryptogramService;
import com.payment.security.service.MACService;
import com.payment.security.service.PINBlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * The issuer is the cardholder's own bank: this is where the final
 * approve/decline decision is made - card status, CVV, funds, and (for the
 * "secure" path) MAC/PIN checks. This is the last hop in the flow diagram
 * before AUTHORIZED / DECLINED.
 */
class CardServiceTest {

    private CardRepository cardRepository;
    private PINBlockService pinBlockService;
    private EMVCryptogramService emvService;
    private MACService macService;
    private Random random;
    private CardService cardService;

    private static final String PAN = "4111111111111111";

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        pinBlockService = mock(PINBlockService.class);
        emvService = mock(EMVCryptogramService.class);
        macService = mock(MACService.class);
        random = mock(Random.class);
        cardService = new CardService(cardRepository, pinBlockService, emvService, macService, random);
        // @Value-injected in production; set directly here since this test
        // builds the service without a Spring context.
        ReflectionTestUtils.setField(cardService, "pinDecryptionKey", "0123456789ABCDEF");
        ReflectionTestUtils.setField(cardService, "macKey", "FEDCBA9876543210");

        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(random.nextInt(1000000)).thenReturn(654321);
    }

    private Card activeCard() {
        Card card = new Card();
        card.setId("card-1");
        card.setCardNumber(PAN);
        card.setCvv("123");
        card.setActive(true);
        card.setBlocked(false);
        card.setAvailableBalance(new BigDecimal("500.00"));
        return card;
    }

    // ---- issueCard ----

    @Test
    void issueCard_appliesADefaultCreditLimitWhenNoneRequested() {
        CardRequest request = new CardRequest();
        request.setCardholderName("Jane Doe");

        CardResponse response = cardService.issueCard(request);

        assertThat(response.getCreditLimit()).isEqualByComparingTo("5000");
    }

    // ---- authorizeTransaction (basic) ----

    @Test
    void authorizeTransaction_declinesWhenCardIsInactive() {
        Card card = activeCard();
        card.setActive(false);
        when(cardRepository.findByCardNumber(PAN)).thenReturn(Optional.of(card));

        AuthorizationResponse response = cardService.authorizeTransaction(
                AuthorizationRequest.builder().cardNumber(PAN).cvv("123").amount(new BigDecimal("50")).build());

        assertThat(response.isApproved()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Card is not active");
    }

    @Test
    void authorizeTransaction_declinesWhenCardIsBlocked() {
        Card card = activeCard();
        card.setBlocked(true);
        when(cardRepository.findByCardNumber(PAN)).thenReturn(Optional.of(card));

        AuthorizationResponse response = cardService.authorizeTransaction(
                AuthorizationRequest.builder().cardNumber(PAN).cvv("123").amount(new BigDecimal("50")).build());

        assertThat(response.isApproved()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Card is blocked");
    }

    @Test
    void authorizeTransaction_declinesOnCvvMismatch() {
        when(cardRepository.findByCardNumber(PAN)).thenReturn(Optional.of(activeCard()));

        AuthorizationResponse response = cardService.authorizeTransaction(
                AuthorizationRequest.builder().cardNumber(PAN).cvv("999").amount(new BigDecimal("50")).build());

        assertThat(response.isApproved()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Invalid CVV");
    }

    @Test
    void authorizeTransaction_declinesWhenBalanceIsInsufficient() {
        when(cardRepository.findByCardNumber(PAN)).thenReturn(Optional.of(activeCard()));

        AuthorizationResponse response = cardService.authorizeTransaction(
                AuthorizationRequest.builder().cardNumber(PAN).cvv("123").amount(new BigDecimal("999999")).build());

        assertThat(response.isApproved()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Insufficient funds");
    }

    @Test
    void authorizeTransaction_approvesAndHoldsFundsWhenEverythingChecksOut() {
        Card card = activeCard();
        when(cardRepository.findByCardNumber(PAN)).thenReturn(Optional.of(card));

        AuthorizationResponse response = cardService.authorizeTransaction(
                AuthorizationRequest.builder().cardNumber(PAN).cvv("123").amount(new BigDecimal("100")).build());

        assertThat(response.isApproved()).isTrue();
        assertThat(response.getResponseCode()).isEqualTo("00");
        assertThat(response.getAuthorizationCode()).isEqualTo("654321");
        assertThat(card.getAvailableBalance()).isEqualByComparingTo("400.00");
        verify(cardRepository).save(card);
    }

    // ---- authorizeTransactionSecure ----

    @Test
    void authorizeTransactionSecure_declinesOnInvalidMacWithoutTouchingTheCard() {
        when(macService.verifyMAC(anyString(), eq("BADMAC"), anyString())).thenReturn(false);

        AuthorizationResponse response = cardService.authorizeTransactionSecure(
                AuthorizationRequest.builder()
                        .cardNumber(PAN).cvv("123").amount(new BigDecimal("50"))
                        .merchantId("merchant-1").mac("BADMAC")
                        .build());

        assertThat(response.isApproved()).isFalse();
        assertThat(response.getResponseCode()).isEqualTo("63");
        verifyNoInteractions(cardRepository);
    }

    @Test
    void authorizeTransactionSecure_declinesOnInvalidPin() {
        Card card = activeCard();
        when(cardRepository.findByCardNumber(PAN)).thenReturn(Optional.of(card));
        when(pinBlockService.verifyPIN(anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        AuthorizationResponse response = cardService.authorizeTransactionSecure(
                AuthorizationRequest.builder()
                        .cardNumber(PAN).cvv("123").amount(new BigDecimal("50"))
                        .encryptedPIN("AABBCCDDEEFF0011")
                        .build());

        assertThat(response.isApproved()).isFalse();
        assertThat(response.getResponseCode()).isEqualTo("55");
    }

    @Test
    void authorizeTransactionSecure_approvesWhenMacAndPinAreValid() {
        Card card = activeCard();
        when(cardRepository.findByCardNumber(PAN)).thenReturn(Optional.of(card));
        when(macService.verifyMAC(anyString(), eq("GOODMAC"), anyString())).thenReturn(true);
        when(pinBlockService.verifyPIN(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

        AuthorizationResponse response = cardService.authorizeTransactionSecure(
                AuthorizationRequest.builder()
                        .cardNumber(PAN).cvv("123").amount(new BigDecimal("100"))
                        .merchantId("merchant-1").mac("GOODMAC")
                        .encryptedPIN("AABBCCDDEEFF0011")
                        .build());

        assertThat(response.isApproved()).isTrue();
        assertThat(response.getResponseCode()).isEqualTo("00");
        assertThat(card.getAvailableBalance()).isEqualByComparingTo("400.00");
    }
}
