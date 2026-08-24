package com.payment.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published by transaction-service to the "transaction.completed" queue
 * once a transaction reaches a terminal state (AUTHORIZED, DECLINED, or
 * FAILED), so settlement/reporting/notification-service can react without
 * being on the synchronous authorization path - the same shape a real
 * payment platform would put on Kafka/RabbitMQ for this. Here it travels
 * over an embedded ActiveMQ Artemis broker (see transaction-service's
 * application.yml) instead of a standalone message broker cluster, which
 * is the one deliberate simplification for running without Docker.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent {
    private String transactionId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String responseCode;
    private String responseMessage;
    private String authorizationCode;
    private LocalDateTime occurredAt;
}
