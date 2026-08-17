package com.payment.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStepDto {
    private String stepName;
    private String target;
    private String status;
    private String detail;
    private Long durationMs;
    private LocalDateTime timestamp;
}
