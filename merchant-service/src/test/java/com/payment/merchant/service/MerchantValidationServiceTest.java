package com.payment.merchant.service;

import com.payment.common.enums.MerchantStatus;
import com.payment.merchant.model.Merchant;
import com.payment.merchant.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Before a transaction is even sent to an acquirer, the merchant itself has
 * to be in good standing and within its risk limits. This is the very first
 * gate in the payment flow diagram: POS -> Merchant Service -> ...
 */
class MerchantValidationServiceTest {

    private MerchantRepository merchantRepository;
    private MerchantValidationService validationService;

    @BeforeEach
    void setUp() {
        merchantRepository = mock(MerchantRepository.class);
        validationService = new MerchantValidationService(merchantRepository);
    }

    private Merchant.MerchantBuilder activeMerchant() {
        return Merchant.builder()
                .id("merchant-1")
                .businessName("Coffee Shop")
                .status(MerchantStatus.ACTIVE)
                .dailyLimit(new BigDecimal("10000"))
                .monthlyLimit(new BigDecimal("100000"))
                .currentDailyVolume(new BigDecimal("500"))
                .currentMonthlyVolume(new BigDecimal("5000"));
    }

    @Test
    void validateMerchant_throwsWhenMerchantDoesNotExist() {
        when(merchantRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> validationService.validateMerchant("missing", BigDecimal.TEN));
    }

    @Test
    void validateMerchant_failsWhenMerchantIsNotActive() {
        Merchant merchant = activeMerchant().status(MerchantStatus.SUSPENDED).build();
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));

        MerchantValidationService.ValidationResult result =
                validationService.validateMerchant("merchant-1", new BigDecimal("50"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("not active");
    }

    @Test
    void validateMerchant_failsWhenDailyLimitWouldBeExceeded() {
        Merchant merchant = activeMerchant().build(); // 500 used of 10000 daily
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));

        MerchantValidationService.ValidationResult result =
                validationService.validateMerchant("merchant-1", new BigDecimal("9600"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("Daily limit exceeded");
    }

    @Test
    void validateMerchant_failsWhenMonthlyLimitWouldBeExceeded() {
        Merchant merchant = activeMerchant()
                .dailyLimit(new BigDecimal("1000000")) // daily won't be the blocker
                .currentMonthlyVolume(new BigDecimal("99900"))
                .build();
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));

        MerchantValidationService.ValidationResult result =
                validationService.validateMerchant("merchant-1", new BigDecimal("200"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("Monthly limit exceeded");
    }

    @Test
    void validateMerchant_succeedsWhenWithinAllLimits() {
        Merchant merchant = activeMerchant().build();
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));

        MerchantValidationService.ValidationResult result =
                validationService.validateMerchant("merchant-1", new BigDecimal("50"));

        assertThat(result.isValid()).isTrue();
        assertThat(result.getMerchant()).isSameAs(merchant);
        verify(merchantRepository, never()).save(any());
    }
}
