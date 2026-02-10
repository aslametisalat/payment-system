package com.payment.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotBlank(message = "Recipient is required")
    private String to;
    
    private String subject;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    @NotBlank(message = "Notification type is required")
    private String type; // EMAIL, SMS, PUSH
    
    private String priority; // LOW, MEDIUM, HIGH
    private String templateId;
}