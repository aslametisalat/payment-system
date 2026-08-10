package com.payment.acquirer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.Random;
@SpringBootApplication
public class AcquirerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcquirerServiceApplication.class, args);
    }

    @Bean
    public Random random() {
        return new Random();
    }
}
