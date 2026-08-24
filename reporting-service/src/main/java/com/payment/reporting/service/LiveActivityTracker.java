package com.payment.reporting.service;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A running tally, per merchant, built entirely from
 * "transaction.completed" events - reporting-service's REST endpoints
 * (TransactionReport etc.) compute everything on demand via Feign calls to
 * transaction-service/merchant-service instead, since those need to be
 * correct for any date range, not just "since this process started". This
 * is the one thing an async event genuinely buys here: a live, in-memory
 * view that updates itself with zero extra API calls, the way a real-time
 * ops dashboard would use it - reset on restart, by design, since it's not
 * meant to be a system of record.
 */
@Component
public class LiveActivityTracker {

    private final Map<String, MerchantActivity> activityByMerchant = new ConcurrentHashMap<>();

    public void record(String merchantId, String status, BigDecimal amount) {
        activityByMerchant.compute(merchantId, (id, existing) -> {
            MerchantActivity activity = existing != null ? existing : new MerchantActivity();
            activity.setTotalTransactions(activity.getTotalTransactions() + 1);
            if ("AUTHORIZED".equals(status) && amount != null) {
                activity.setAuthorizedTransactions(activity.getAuthorizedTransactions() + 1);
                activity.setAuthorizedAmount(activity.getAuthorizedAmount().add(amount));
            } else if ("DECLINED".equals(status)) {
                activity.setDeclinedTransactions(activity.getDeclinedTransactions() + 1);
            } else if ("FAILED".equals(status)) {
                activity.setFailedTransactions(activity.getFailedTransactions() + 1);
            }
            return activity;
        });
    }

    public Map<String, MerchantActivity> snapshot() {
        return Map.copyOf(activityByMerchant);
    }

    @Data
    public static class MerchantActivity {
        private long totalTransactions = 0;
        private long authorizedTransactions = 0;
        private long declinedTransactions = 0;
        private long failedTransactions = 0;
        private BigDecimal authorizedAmount = BigDecimal.ZERO;
    }
}
