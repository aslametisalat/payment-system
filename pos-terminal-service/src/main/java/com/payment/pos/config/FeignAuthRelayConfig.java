package com.payment.pos.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Relays the caller's bearer token onto the outgoing call to
 * transaction-service - see transaction-service's identical class for why.
 */
@Configuration
public class FeignAuthRelayConfig {

    @Bean
    public RequestInterceptor authRelayInterceptor() {
        return requestTemplate -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String authHeader = attrs.getRequest().getHeader("Authorization");
                if (authHeader != null) {
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }
}
