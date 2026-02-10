package com.payment.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String notificationId;
    private String to;
    private String type;
    private String status; // SENT, FAILED, PENDING
    private String message;
    private LocalDateTime sentAt;
}