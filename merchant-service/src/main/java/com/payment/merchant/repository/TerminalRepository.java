package com.payment.merchant.repository;

import com.payment.merchant.model.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, String> {
    
    List<Terminal> findByMerchantId(String merchantId);
    
    Optional<Terminal> findByTerminalId(String terminalId);
    
    List<Terminal> findByMerchantIdAndActive(String merchantId, Boolean active);
    
    boolean existsByTerminalId(String terminalId);
}