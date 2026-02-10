package com.payment.notification.service;
import com.payment.notification.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    
    public void sendNotification(NotificationRequest request) {
        log.info("Sending {} notification to: {}", request.getType(), request.getTo());
        log.info("Subject: {}", request.getSubject());
        log.info("Message: {}", request.getMessage());
        // Implement actual notification logic (email, SMS, push)
    }
    
    public void sendTransactionAlert(String merchantId, String transactionId, String status) {
        log.info("Transaction alert - Merchant: {}, Transaction: {}, Status: {}", 
            merchantId, transactionId, status);
    }
}
