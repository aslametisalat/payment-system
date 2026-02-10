package com.payment.settlement.service;
import com.payment.settlement.model.Settlement;
import com.payment.settlement.repository.SettlementRepository;
import com.payment.common.enums.SettlementStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {
    private final SettlementRepository repository;
    
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void processSettlements() {
        log.info("Starting daily settlement process");
        // Settlement logic here
    }
    
    public Settlement createSettlement(String merchantId, BigDecimal amount, int count) {
        Settlement settlement = new Settlement();
        settlement.setMerchantId(merchantId);
        settlement.setSettlementDate(LocalDate.now());
        settlement.setTotalAmount(amount);
        settlement.setFees(amount.multiply(new BigDecimal("0.02"))); // 2% fee
        settlement.setNetAmount(amount.subtract(settlement.getFees()));
        settlement.setTransactionCount(count);
        settlement.setStatus(SettlementStatus.PENDING);
        return repository.save(settlement);
    }
    
    public List<Settlement> getMerchantSettlements(String merchantId) {
        return repository.findByMerchantId(merchantId);
    }
}
