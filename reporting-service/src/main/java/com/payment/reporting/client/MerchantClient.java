package com.payment.reporting.client;

import com.payment.reporting.dto.MerchantSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "merchant-service")
public interface MerchantClient {

    @GetMapping("/api/merchants/{id}")
    MerchantSummary getMerchant(@PathVariable("id") String id);
}
