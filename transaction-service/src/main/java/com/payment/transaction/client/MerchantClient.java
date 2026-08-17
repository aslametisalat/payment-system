package com.payment.transaction.client;

import com.payment.transaction.dto.MerchantValidationResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "merchant-service")
public interface MerchantClient {

    @PostMapping("/api/merchants/{id}/validate")
    MerchantValidationResult validateForTransaction(
            @PathVariable("id") String merchantId,
            @RequestParam("amount") BigDecimal amount);
}
