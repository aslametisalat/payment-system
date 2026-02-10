package com.payment.merchant.service;

import com.payment.merchant.dto.MerchantRequest;
import com.payment.merchant.dto.MerchantResponse;
import com.payment.merchant.model.Merchant;
import com.payment.merchant.repository.MerchantRepository;
import com.payment.common.enums.MerchantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantService {
    
    private final MerchantRepository merchantRepository;
    
    @Transactional
    public MerchantResponse createMerchant(MerchantRequest request) {
        log.info("Creating merchant: {}", request.getBusinessName());
        
        // Check if email already exists
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Merchant with email already exists: " + request.getEmail());
        }
        
        // Check if tax ID already exists
        if (merchantRepository.existsByTaxId(request.getTaxId())) {
            throw new RuntimeException("Merchant with tax ID already exists: " + request.getTaxId());
        }
        
        Merchant merchant = Merchant.builder()
                .businessName(request.getBusinessName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .taxId(request.getTaxId())
                .merchantCategoryCode(request.getMerchantCategoryCode())
                .status(MerchantStatus.PENDING_VERIFICATION)
                .dailyLimit(request.getDailyLimit())
                .monthlyLimit(request.getMonthlyLimit())
                .currentDailyVolume(BigDecimal.ZERO)
                .currentMonthlyVolume(BigDecimal.ZERO)
                .accountNumber(request.getAccountNumber())
                .routingNumber(request.getRoutingNumber())
                .bankName(request.getBankName())
                .build();
        
        merchant = merchantRepository.save(merchant);
        log.info("Merchant created with ID: {}", merchant.getId());
        
        return toResponse(merchant);
    }
    
    @Transactional(readOnly = true)
    public List<MerchantResponse> getAllMerchants() {
        log.info("Fetching all merchants");
        return merchantRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public MerchantResponse getMerchantById(String id) {
        log.info("Fetching merchant by ID: {}", id);
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + id));
        return toResponse(merchant);
    }
    
    @Transactional
    public MerchantResponse updateMerchantStatus(String id, MerchantStatus status) {
        log.info("Updating merchant {} status to {}", id, status);
        
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + id));
        
        merchant.setStatus(status);
        merchant = merchantRepository.save(merchant);
        
        log.info("Merchant status updated successfully");
        return toResponse(merchant);
    }
    
    @Transactional
    public ValidationResult validateForTransaction(String merchantId, BigDecimal transactionAmount) {
        log.info("╔═══════════════════════════════════════╗");
        log.info("║  MERCHANT VALIDATION                  ║");
        log.info("╚═══════════════════════════════════════╝");
        log.info("Merchant ID: {}", merchantId);
        log.info("Transaction Amount: ${}", transactionAmount);
        
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + merchantId));
        
        log.info("Merchant: {}", merchant.getBusinessName());
        log.info("Status: {}", merchant.getStatus());
        
        // Check 1: Active status
        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            log.warn("❌ Merchant not active: {}", merchant.getStatus());
            return ValidationResult.failed("Merchant account not active: " + merchant.getStatus());
        }
        log.info("✓ Merchant is ACTIVE");
        
        // Check 2: Daily limit
        BigDecimal newDailyVolume = merchant.getCurrentDailyVolume().add(transactionAmount);
        log.info("Daily Volume Check:");
        log.info("  Current: ${}", merchant.getCurrentDailyVolume());
        log.info("  Limit: ${}", merchant.getDailyLimit());
        log.info("  After transaction: ${}", newDailyVolume);
        
        if (newDailyVolume.compareTo(merchant.getDailyLimit()) > 0) {
            log.warn("❌ Daily limit would be exceeded");
            return ValidationResult.failed("Daily limit exceeded");
        }
        log.info("✓ Within daily limit");
        
        // Check 3: Monthly limit
        BigDecimal newMonthlyVolume = merchant.getCurrentMonthlyVolume().add(transactionAmount);
        log.info("Monthly Volume Check:");
        log.info("  Current: ${}", merchant.getCurrentMonthlyVolume());
        log.info("  Limit: ${}", merchant.getMonthlyLimit());
        log.info("  After transaction: ${}", newMonthlyVolume);
        
        if (newMonthlyVolume.compareTo(merchant.getMonthlyLimit()) > 0) {
            log.warn("❌ Monthly limit would be exceeded");
            return ValidationResult.failed("Monthly limit exceeded");
        }
        log.info("✓ Within monthly limit");
        
        // Update volumes
        merchant.setCurrentDailyVolume(newDailyVolume);
        merchant.setCurrentMonthlyVolume(newMonthlyVolume);
        merchantRepository.save(merchant);
        
        log.info("✓ Merchant validation PASSED");
        log.info("Updated daily volume: ${}", newDailyVolume);
        
        return ValidationResult.success(merchant);
    }
    
    @Transactional
    public void resetDailyVolumes() {
        log.info("Resetting daily volumes for all merchants");
        List<Merchant> merchants = merchantRepository.findAll();
        merchants.forEach(merchant -> {
            merchant.setCurrentDailyVolume(BigDecimal.ZERO);
            merchantRepository.save(merchant);
        });
        log.info("Daily volumes reset for {} merchants", merchants.size());
    }
    
    private MerchantResponse toResponse(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getId())
                .businessName(merchant.getBusinessName())
                .email(merchant.getEmail())
                .phone(merchant.getPhone())
                .address(merchant.getAddress())
                .status(merchant.getStatus())
                .merchantCategoryCode(merchant.getMerchantCategoryCode())
                .dailyLimit(merchant.getDailyLimit())
                .monthlyLimit(merchant.getMonthlyLimit())
                .currentDailyVolume(merchant.getCurrentDailyVolume())
                .currentMonthlyVolume(merchant.getCurrentMonthlyVolume())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .build();
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