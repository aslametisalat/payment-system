package com.payment.settlement.listener;

import com.payment.common.event.TransactionCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to transactions asynchronously, off the authorization path -
 * transaction-service publishes to "transaction.completed" the moment a
 * transaction reaches a terminal state (see its
 * TransactionProcessingService.publishCompletionEvent()) without waiting
 * for, or caring whether, this listener is even running.
 *
 * Settlement itself stays a nightly batch (see SettlementService's
 * @Scheduled cron) rather than something this listener triggers per
 * transaction - a settlement run aggregates a whole day's approved
 * transactions per merchant, so re-running it after every single one would
 * either do nothing (already-settled-today guard) or fight the batch's own
 * "does today's settlement already exist" check. What this listener adds is
 * the thing a real settlement system genuinely does per transaction: record
 * that it landed and will be picked up.
 */
@Component
@Slf4j
public class TransactionCompletedListener {

    @JmsListener(destination = "transaction.completed.settlement")
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        if (!"AUTHORIZED".equals(event.getStatus())) {
            log.debug("Transaction {} ({}) is not settleable - ignoring", event.getTransactionId(), event.getStatus());
            return;
        }
        log.info("Transaction {} for merchant {} (${}) authorized - queued for the next settlement run",
                event.getTransactionId(), event.getMerchantId(), event.getAmount());
    }
}
