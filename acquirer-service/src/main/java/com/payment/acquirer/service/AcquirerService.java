package com.payment.acquirer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class AcquirerService {

    private final Random random;

    /**
     * Process acquiring - fraud checks, routing
     */
    public AcquiringResult processAcquiring(String merchantId, String cardNumber, 
                                           BigDecimal amount) {
        log.info("Processing acquiring for merchant: {}", merchantId);
        
        // Fraud screening
        int fraudScore = performFraudCheck(cardNumber, amount);
        log.debug("Fraud score: {}", fraudScore);
        
        if (fraudScore > 80) {
            log.warn("High fraud score detected: {}", fraudScore);
            return AcquiringResult.declined("Suspected fraud");
        }
        
        // Velocity check
        if (!checkVelocity(cardNumber)) {
            log.warn("Velocity check failed");
            return AcquiringResult.declined("Too many transactions");
        }
        
        log.info("Acquiring checks passed, routing to network");
        return AcquiringResult.approved();
    }
    
    // Package-private (not private) so unit tests can exercise the scoring
    // formula directly with a controlled Random, without needing to reach
    // it indirectly through processAcquiring().
    int performFraudCheck(String cardNumber, BigDecimal amount) {
        // Simplified fraud scoring
        int score = 0;

        // High amount adds risk
        if (amount.compareTo(new BigDecimal("1000")) > 0) {
            score += 20;
        }

        // Random component for simulation
        score += random.nextInt(30);

        return score;
    }

    boolean checkVelocity(String cardNumber) {
        // In real system, check transaction frequency
        // For simulation, always pass
        return true;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AcquiringResult {
        private boolean approved;
        private String message;
        
        public static AcquiringResult approved() {
            return new AcquiringResult(true, "Approved");
        }
        
        public static AcquiringResult declined(String reason) {
            return new AcquiringResult(false, reason);
        }
    }
}