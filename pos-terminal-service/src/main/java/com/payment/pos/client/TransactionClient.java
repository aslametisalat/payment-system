package com.payment.pos.client;

import com.payment.pos.dto.TransactionRequest;
import com.payment.pos.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "transaction-service")
public interface TransactionClient {

    @PostMapping("/api/transactions/authorize")
    TransactionResponse authorize(@RequestBody TransactionRequest request);
}
