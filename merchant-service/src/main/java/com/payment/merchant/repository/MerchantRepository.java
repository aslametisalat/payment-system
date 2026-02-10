package com.payment.merchant.repository;

import com.payment.merchant.model.Merchant;
import com.payment.common.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {
    
    Optional<Merchant> findByEmail(String email);
    
    List<Merchant> findByStatus(MerchantStatus status);
    
    List<Merchant> findByBusinessNameContainingIgnoreCase(String businessName);
    
    boolean existsByEmail(String email);
    
    boolean existsByTaxId(String taxId);
}