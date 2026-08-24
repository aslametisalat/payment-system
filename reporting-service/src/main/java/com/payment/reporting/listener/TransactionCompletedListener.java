package com.payment.reporting.listener;

import com.payment.common.event.TransactionCompletedEvent;
import com.payment.reporting.service.LiveActivityTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Feeds LiveActivityTracker off transaction-service's
 * "transaction.completed" queue - see that class for why this is a live
 * tally rather than a system of record.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCompletedListener {

    private final LiveActivityTracker activityTracker;

    @JmsListener(destination = "transaction.completed.reporting")
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        activityTracker.record(event.getMerchantId(), event.getStatus(), event.getAmount());
        log.debug("Recorded {} transaction {} for merchant {} in live activity tracker",
                event.getStatus(), event.getTransactionId(), event.getMerchantId());
    }
}
