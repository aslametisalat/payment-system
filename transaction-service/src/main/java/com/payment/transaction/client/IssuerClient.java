package com.payment.transaction.client;

import com.payment.transaction.dto.AuthorizationRequest;
import com.payment.transaction.dto.AuthorizationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "issuer-service", path = "/api/cards")
public interface IssuerClient {
    @PostMapping("/authorize")
    AuthorizationResponse authorize(@RequestBody AuthorizationRequest request);
}
