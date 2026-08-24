package com.payment.network;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
@SpringBootApplication
@ComponentScan(basePackages = {"com.payment.network", "com.payment.common"})
public class NetworkServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetworkServiceApplication.class, args);
    }
}
