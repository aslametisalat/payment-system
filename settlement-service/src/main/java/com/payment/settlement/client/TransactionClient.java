package com.payment.settlement.client;

import com.payment.settlement.dto.TransactionSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "transaction-service")
public interface TransactionClient {

    @GetMapping("/api/transactions/merchant/{merchantId}")
    List<TransactionSummary> getMerchantTransactions(@PathVariable("merchantId") String merchantId);
}
