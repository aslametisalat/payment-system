package com.payment.transaction.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Relays the caller's bearer token onto every outgoing Feign call - the
 * same standard "token relay" pattern an API gateway uses, just done here
 * because this project doesn't route everything through one. Without this,
 * a validly-authenticated call into transaction-service would fail at
 * merchant/acquirer/network/issuer-service, since JwtAuthenticationFilter
 * is enforcing the same bearer-token requirement there too - re-using the
 * one token the caller already presented is what makes that work, rather
 * than each service needing its own credentials for every other service.
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
