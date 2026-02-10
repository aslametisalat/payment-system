package com.payment.merchant.service;

import com.payment.merchant.model.Merchant;
import com.payment.merchant.repository.MerchantRepository;
import com.payment.common.enums.MerchantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantValidationService {
    
    private final MerchantRepository merchantRepository;
    
    /**
     * Validate merchant can process transaction
     */
    public ValidationResult validateMerchant(String merchantId, BigDecimal amount) {
        log.info("Validating merchant: {}", merchantId);
        
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new RuntimeException("Merchant not found: " + merchantId));
        
        // Check if active
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            log.warn("Merchant not active: {}", merchant.getStatus());
            return ValidationResult.failed("Merchant account not active");
        }
        
        // Check daily limit
        BigDecimal newDailyVolume = merchant.getCurrentDailyVolume().add(amount);
        if (newDailyVolume.compareTo(merchant.getDailyLimit()) > 0) {
            log.warn("Daily limit exceeded: {} > {}", newDailyVolume, merchant.getDailyLimit());
            return ValidationResult.failed("Daily limit exceeded");
        }
        
        // Check monthly limit
        BigDecimal newMonthlyVolume = merchant.getCurrentMonthlyVolume().add(amount);
        if (newMonthlyVolume.compareTo(merchant.getMonthlyLimit()) > 0) {
            log.warn("Monthly limit exceeded: {} > {}", newMonthlyVolume, merchant.getMonthlyLimit());
            return ValidationResult.failed("Monthly limit exceeded");
        }
        
        log.info("Merchant validation passed");
        return ValidationResult.success(merchant);
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private String message;
        private Merchant merchant;
        
        public static ValidationResult success(Merchant merchant) {
            return new ValidationResult(true, "Validation passed", merchant);
        }
        
        public static ValidationResult failed(String message) {
            return new ValidationResult(false, message, null);
        }
    }
}