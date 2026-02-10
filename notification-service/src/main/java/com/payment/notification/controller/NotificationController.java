package com.payment.notification.controller;

import com.payment.notification.dto.NotificationRequest;
import com.payment.notification.dto.NotificationResponse;
import com.payment.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification Management Operations")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * Send a notification
     */
    @PostMapping("/send")
    @Operation(summary = "Send notification", 
               description = "Send notification via email, SMS, or push")
    public ResponseEntity<Void> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("╔═══════════════════════════════════════════════╗");
        log.info("║  SENDING NOTIFICATION                         ║");
        log.info("╠═══════════════════════════════════════════════╣");
        log.info("║  To: {}", String.format("%-39s", request.getTo()) + "║");
        log.info("║  Type: {}", String.format("%-37s", request.getType()) + "║");
        log.info("║  Subject: {}", String.format("%-34s", 
            request.getSubject() != null ? request.getSubject() : "N/A") + "║");
        log.info("╚═══════════════════════════════════════════════╝");
        
        notificationService.sendNotification(request);
        return ResponseEntity.ok().build(); // Method returns void
    }
    
    /**
     * Send transaction alert
     */
    @PostMapping("/transaction-alert")
    @Operation(summary = "Send transaction alert", 
               description = "Send notification for transaction event")
    public ResponseEntity<Void> sendTransactionAlert(
            @RequestParam String to,
            @RequestParam String transactionId,
            @RequestParam String amount,
            @RequestParam String status) {
        
        String message = String.format(
            "Transaction %s for amount $%s - Status: %s",
            transactionId, amount, status
        );
        
        NotificationRequest request = NotificationRequest.builder()
            .to(to)
            .subject("Transaction Alert")
            .message(message)
            .type("EMAIL")
            .build();
        
        notificationService.sendNotification(request);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Send settlement notification
     */
    @PostMapping("/settlement-alert")
    @Operation(summary = "Send settlement alert")
    public ResponseEntity<Void> sendSettlementAlert(
            @RequestParam String merchantEmail,
            @RequestParam String settlementId,
            @RequestParam String amount) {
        
        String message = String.format(
            "Your settlement %s of $%s has been processed and will be deposited within 1-2 business days.",
            settlementId, amount
        );
        
        NotificationRequest request = NotificationRequest.builder()
            .to(merchantEmail)
            .subject("Settlement Processed")
            .message(message)
            .type("EMAIL")
            .build();
        
        notificationService.sendNotification(request);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get notification history
     */
    @GetMapping("/history/{recipient}")
    @Operation(summary = "Get notification history")
    public ResponseEntity<List<NotificationResponse>> getNotificationHistory(
            @PathVariable String recipient) {
        
        log.info("Fetching notification history for: {}", recipient);
        // List<NotificationResponse> history = notificationService.getNotificationHistory(recipient);
        // return ResponseEntity.ok(history);
        return ResponseEntity.ok(List.of()); // Method not implemented
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service Operational");
    }
}