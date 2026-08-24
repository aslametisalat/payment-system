package com.payment.issuer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import java.util.Random;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.payment.issuer", "com.payment.security", "com.payment.common"})
public class IssuerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IssuerServiceApplication.class, args);
    }

    @Bean
    public Random random() {
        return new Random();
    }
}
