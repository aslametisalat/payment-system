package com.payment.transaction.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * One hop of a transaction's journey through the system - which service was
 * called, whether it succeeded/declined/failed, and how long it took. A
 * transaction accumulates one of these per stage (merchant, acquirer,
 * network, issuer) so the full path can be inspected after the fact instead
 * of only the final outcome.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStep {
    private String stepName;
    private String target;
    private String status;
    private String detail;
    private Long durationMs;
    private LocalDateTime timestamp;
}
