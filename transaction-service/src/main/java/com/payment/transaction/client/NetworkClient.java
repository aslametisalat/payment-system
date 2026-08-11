package com.payment.transaction.client;

import com.payment.transaction.dto.RoutingRequest;
import com.payment.transaction.dto.RoutingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "network-service")
public interface NetworkClient {

    @PostMapping("/api/network/route")
    RoutingResponse route(@RequestBody RoutingRequest request);
}
