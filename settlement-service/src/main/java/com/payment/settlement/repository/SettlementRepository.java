package com.payment.settlement.repository;
import com.payment.settlement.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, String> {
    List<Settlement> findByMerchantId(String merchantId);
    List<Settlement> findBySettlementDate(LocalDate date);
    boolean existsByMerchantIdAndSettlementDate(String merchantId, LocalDate settlementDate);
}
