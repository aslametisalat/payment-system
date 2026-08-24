package com.payment.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Dev-only CORS: lets the static dashboard (served from pos-terminal-service,
 * a different port) fetch a demo token straight from the browser. No
 * credentials/cookies are involved, so allowing any origin is fine for a
 * local learning tool - this is not meant for production use.
 */
@Configuration
public class SecurityCorsConfig {
    // Named uniquely (not corsConfigurer(), the convention every other
    // service's own WebConfig uses) because issuer-service and
    // pos-terminal-service component-scan com.payment.security broadly
    // (for MACService/PINBlockService/EMVCryptogramService) and would
    // otherwise pick this bean up too, colliding by @Bean method name with
    // their own CORS config.
    @Bean
    public WebMvcConfigurer securityServiceCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }
}
