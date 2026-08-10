package com.payment.notification.service;
import com.payment.notification.dto.NotificationRequest;
import com.payment.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    // In-memory notification log, keyed by recipient (simulation only - no external email/SMS/push provider wired up)
    private final Map<String, List<NotificationResponse>> notificationHistory = new ConcurrentHashMap<>();

    public void sendNotification(NotificationRequest request) {
        log.info("Sending {} notification to: {}", request.getType(), request.getTo());
        log.info("Subject: {}", request.getSubject());
        log.info("Message: {}", request.getMessage());
        // Simulation only: real system would dispatch via an email/SMS/push provider here

        NotificationResponse record = NotificationResponse.builder()
                .notificationId(UUID.randomUUID().toString())
                .to(request.getTo())
                .type(request.getType())
                .status("SENT")
                .message(request.getMessage())
                .sentAt(LocalDateTime.now())
                .build();

        notificationHistory
                .computeIfAbsent(request.getTo(), key -> new CopyOnWriteArrayList<>())
                .add(record);
    }

    public void sendTransactionAlert(String merchantId, String transactionId, String status) {
        log.info("Transaction alert - Merchant: {}, Transaction: {}, Status: {}",
            merchantId, transactionId, status);
    }

    public List<NotificationResponse> getNotificationHistory(String recipient) {
        return notificationHistory.getOrDefault(recipient, List.of()).stream()
                .sorted(Comparator.comparing(NotificationResponse::getSentAt).reversed())
                .collect(Collectors.toList());
    }
}
