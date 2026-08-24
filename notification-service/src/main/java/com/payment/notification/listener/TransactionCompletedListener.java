package com.payment.notification.listener;

import com.payment.common.event.TransactionCompletedEvent;
import com.payment.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * The one real consumer of transaction-service's "transaction.completed"
 * queue: NotificationService.sendTransactionAlert() already existed for
 * exactly this purpose, but nothing ever called it before this - it was
 * dead code, only reachable from a unit test. This is the real "your card
 * was charged" / "your payment was declined" step of a card transaction,
 * running asynchronously and not risking the actual authorization if it's
 * slow or (like every other consumer here) simply not running.
 */
@Component
@RequiredArgsConstructor
public class TransactionCompletedListener {

    private final NotificationService notificationService;

    @JmsListener(destination = "transaction.completed.notification")
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        notificationService.sendTransactionAlert(
                event.getMerchantId(), event.getTransactionId(), event.getStatus());
    }
}
