package com.payment.merchant.service;

import com.payment.common.enums.MerchantStatus;
import com.payment.merchant.dto.MerchantRequest;
import com.payment.merchant.dto.MerchantResponse;
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

class MerchantServiceTest {

    private MerchantRepository merchantRepository;
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        merchantRepository = mock(MerchantRepository.class);
        merchantService = new MerchantService(merchantRepository);
        // Persist just echoes back whatever was passed in, like a real JPA save would.
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));
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
    void createMerchant_rejectsADuplicateEmail() {
        MerchantRequest request = new MerchantRequest();
        request.setEmail("shop@coffee.com");
        request.setTaxId("TAX-1");
        when(merchantRepository.existsByEmail("shop@coffee.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> merchantService.createMerchant(request));
        verify(merchantRepository, never()).save(any());
    }

    @Test
    void createMerchant_startsNewMerchantsAtZeroVolumeAndPendingVerification() {
        MerchantRequest request = new MerchantRequest();
        request.setBusinessName("Coffee Shop");
        request.setEmail("shop@coffee.com");
        request.setTaxId("TAX-1");
        request.setDailyLimit(new BigDecimal("10000"));
        request.setMonthlyLimit(new BigDecimal("100000"));

        MerchantResponse response = merchantService.createMerchant(request);

        assertThat(response.getStatus()).isEqualTo(MerchantStatus.PENDING_VERIFICATION);
        assertThat(response.getCurrentDailyVolume()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCurrentMonthlyVolume()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void validateForTransaction_declinesWhenMerchantIsNotActive() {
        Merchant merchant = activeMerchant().status(MerchantStatus.SUSPENDED).build();
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));

        MerchantService.ValidationResult result =
                merchantService.validateForTransaction("merchant-1", new BigDecimal("50"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("not active");
        verify(merchantRepository, never()).save(any());
    }

    @Test
    void validateForTransaction_declinesWhenDailyLimitWouldBeExceeded() {
        Merchant merchant = activeMerchant().build(); // 500 of 10000 used
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));

        MerchantService.ValidationResult result =
                merchantService.validateForTransaction("merchant-1", new BigDecimal("9600"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Daily limit exceeded");
    }

    @Test
    void validateForTransaction_approvesAndBumpsVolumesWhenWithinLimits() {
        Merchant merchant = activeMerchant().build();
        when(merchantRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));

        MerchantService.ValidationResult result =
                merchantService.validateForTransaction("merchant-1", new BigDecimal("100"));

        assertThat(result.isValid()).isTrue();
        assertThat(merchant.getCurrentDailyVolume()).isEqualByComparingTo("600");
        assertThat(merchant.getCurrentMonthlyVolume()).isEqualByComparingTo("5100");
        verify(merchantRepository).save(merchant);
    }

    @Test
    void resetDailyVolumes_zeroesOutEveryMerchantsDailyVolume() {
        Merchant merchant = activeMerchant().build();
        when(merchantRepository.findAll()).thenReturn(java.util.List.of(merchant));

        merchantService.resetDailyVolumes();

        assertThat(merchant.getCurrentDailyVolume()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(merchantRepository).save(merchant);
    }
}
