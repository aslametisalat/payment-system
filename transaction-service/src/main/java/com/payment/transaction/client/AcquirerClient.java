package com.payment.transaction.client;

import com.payment.transaction.dto.AcquirerRequest;
import com.payment.transaction.dto.AcquirerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "acquirer-service")
public interface AcquirerClient {

    @PostMapping("/api/acquirers/process")
    AcquirerResponse processAcquiring(@RequestBody AcquirerRequest request);
}
