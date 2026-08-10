package com.payment.settlement.client;

import com.payment.settlement.dto.MerchantSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "merchant-service")
public interface MerchantClient {

    @GetMapping("/api/merchants")
    List<MerchantSummary> getAllMerchants();
}
