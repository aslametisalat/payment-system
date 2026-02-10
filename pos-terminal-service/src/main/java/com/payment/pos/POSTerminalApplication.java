package com.payment.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan(basePackages = {"com.payment.pos", "com.payment.security", "com.payment.iso8583"})
public class POSTerminalApplication {
    public static void main(String[] args) {
        SpringApplication.run(POSTerminalApplication.class, args);
    }
}