#!/bin/bash

BASE_DIR="/home/claude/payment-system"
cd "$BASE_DIR"

echo "=== Creating Acquirer Service ==="

cat > "$BASE_DIR/acquirer-service/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.payment</groupId><artifactId>payment-system</artifactId><version>1.0.0</version></parent>
    <artifactId>acquirer-service</artifactId>
    <dependencies>
        <dependency><groupId>com.payment</groupId><artifactId>common</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-netflix-eureka-client</artifactId></dependency>
        <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
    </dependencies>
</project>
EOF

mkdir -p "$BASE_DIR/acquirer-service/src/main/java/com/payment/acquirer"/{model,repository,service,controller}
mkdir -p "$BASE_DIR/acquirer-service/src/main/resources"

cat > "$BASE_DIR/acquirer-service/src/main/java/com/payment/acquirer/AcquirerServiceApplication.java" << 'EOF'
package com.payment.acquirer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class AcquirerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AcquirerServiceApplication.class, args);
    }
}
EOF

cat > "$BASE_DIR/acquirer-service/src/main/resources/application.yml" << 'EOF'
server:
  port: 8082
spring:
  application:
    name: acquirer-service
  datasource:
    url: jdbc:h2:mem:acquirerdb
  jpa:
    hibernate:
      ddl-auto: create-drop
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
EOF

echo "=== Creating Network Service ==="

cat > "$BASE_DIR/network-service/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.payment</groupId><artifactId>payment-system</artifactId><version>1.0.0</version></parent>
    <artifactId>network-service</artifactId>
    <dependencies>
        <dependency><groupId>com.payment</groupId><artifactId>common</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-netflix-eureka-client</artifactId></dependency>
        <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
    </dependencies>
</project>
EOF

mkdir -p "$BASE_DIR/network-service/src/main/java/com/payment/network"/{model,service,controller}
mkdir -p "$BASE_DIR/network-service/src/main/resources"

cat > "$BASE_DIR/network-service/src/main/java/com/payment/network/NetworkServiceApplication.java" << 'EOF'
package com.payment.network;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class NetworkServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetworkServiceApplication.class, args);
    }
}
EOF

cat > "$BASE_DIR/network-service/src/main/resources/application.yml" << 'EOF'
server:
  port: 8084
spring:
  application:
    name: network-service
  datasource:
    url: jdbc:h2:mem:networkdb
  jpa:
    hibernate:
      ddl-auto: create-drop
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
EOF

echo "=== Creating Settlement Service ==="

cat > "$BASE_DIR/settlement-service/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.payment</groupId><artifactId>payment-system</artifactId><version>1.0.0</version></parent>
    <artifactId>settlement-service</artifactId>
    <dependencies>
        <dependency><groupId>com.payment</groupId><artifactId>common</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-netflix-eureka-client</artifactId></dependency>
        <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
    </dependencies>
</project>
EOF

mkdir -p "$BASE_DIR/settlement-service/src/main/java/com/payment/settlement"/{model,repository,service,controller}
mkdir -p "$BASE_DIR/settlement-service/src/main/resources"

cat > "$BASE_DIR/settlement-service/src/main/java/com/payment/settlement/SettlementServiceApplication.java" << 'EOF'
package com.payment.settlement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class SettlementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SettlementServiceApplication.class, args);
    }
}
EOF

cat > "$BASE_DIR/settlement-service/src/main/java/com/payment/settlement/model/Settlement.java" << 'EOF'
package com.payment.settlement.model;
import com.payment.common.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String merchantId;
    private LocalDate settlementDate;
    private BigDecimal totalAmount;
    private BigDecimal fees;
    private BigDecimal netAmount;
    private int transactionCount;
    @Enumerated(EnumType.STRING)
    private SettlementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = SettlementStatus.PENDING;
    }
}
EOF

cat > "$BASE_DIR/settlement-service/src/main/java/com/payment/settlement/repository/SettlementRepository.java" << 'EOF'
package com.payment.settlement.repository;
import com.payment.settlement.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, String> {
    List<Settlement> findByMerchantId(String merchantId);
    List<Settlement> findBySettlementDate(LocalDate date);
}
EOF

cat > "$BASE_DIR/settlement-service/src/main/java/com/payment/settlement/service/SettlementService.java" << 'EOF'
package com.payment.settlement.service;
import com.payment.settlement.model.Settlement;
import com.payment.settlement.repository.SettlementRepository;
import com.payment.common.enums.SettlementStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {
    private final SettlementRepository repository;
    
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void processSettlements() {
        log.info("Starting daily settlement process");
        // Settlement logic here
    }
    
    public Settlement createSettlement(String merchantId, BigDecimal amount, int count) {
        Settlement settlement = new Settlement();
        settlement.setMerchantId(merchantId);
        settlement.setSettlementDate(LocalDate.now());
        settlement.setTotalAmount(amount);
        settlement.setFees(amount.multiply(new BigDecimal("0.02"))); // 2% fee
        settlement.setNetAmount(amount.subtract(settlement.getFees()));
        settlement.setTransactionCount(count);
        settlement.setStatus(SettlementStatus.PENDING);
        return repository.save(settlement);
    }
    
    public List<Settlement> getMerchantSettlements(String merchantId) {
        return repository.findByMerchantId(merchantId);
    }
}
EOF

cat > "$BASE_DIR/settlement-service/src/main/java/com/payment/settlement/controller/SettlementController.java" << 'EOF'
package com.payment.settlement.controller;
import com.payment.settlement.model.Settlement;
import com.payment.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService service;
    
    @GetMapping("/merchant/{merchantId}")
    public List<Settlement> getMerchantSettlements(@PathVariable String merchantId) {
        return service.getMerchantSettlements(merchantId);
    }
}
EOF

cat > "$BASE_DIR/settlement-service/src/main/resources/application.yml" << 'EOF'
server:
  port: 8086
spring:
  application:
    name: settlement-service
  datasource:
    url: jdbc:h2:mem:settlementdb
  jpa:
    hibernate:
      ddl-auto: create-drop
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
EOF

echo "=== Creating Reporting Service ==="

cat > "$BASE_DIR/reporting-service/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.payment</groupId><artifactId>payment-system</artifactId><version>1.0.0</version></parent>
    <artifactId>reporting-service</artifactId>
    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-netflix-eureka-client</artifactId></dependency>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
    </dependencies>
</project>
EOF

mkdir -p "$BASE_DIR/reporting-service/src/main/java/com/payment/reporting"/{service,controller,dto}
mkdir -p "$BASE_DIR/reporting-service/src/main/resources"

cat > "$BASE_DIR/reporting-service/src/main/java/com/payment/reporting/ReportingServiceApplication.java" << 'EOF'
package com.payment.reporting;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@SpringBootApplication
@EnableFeignClients
public class ReportingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportingServiceApplication.class, args);
    }
}
EOF

cat > "$BASE_DIR/reporting-service/src/main/java/com/payment/reporting/dto/TransactionReport.java" << 'EOF'
package com.payment.reporting.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TransactionReport {
    private long totalTransactions;
    private BigDecimal totalVolume;
    private long approvedCount;
    private long declinedCount;
    private BigDecimal approvalRate;
}
EOF

cat > "$BASE_DIR/reporting-service/src/main/java/com/payment/reporting/service/ReportingService.java" << 'EOF'
package com.payment.reporting.service;
import com.payment.reporting.dto.TransactionReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {
    
    public TransactionReport generateTransactionReport(String merchantId) {
        log.info("Generating report for merchant: {}", merchantId);
        // In real system, aggregate data from transaction service
        return TransactionReport.builder()
            .totalTransactions(100)
            .totalVolume(new BigDecimal("50000.00"))
            .approvedCount(95)
            .declinedCount(5)
            .approvalRate(new BigDecimal("95.0"))
            .build();
    }
}
EOF

cat > "$BASE_DIR/reporting-service/src/main/java/com/payment/reporting/controller/ReportController.java" << 'EOF'
package com.payment.reporting.controller;
import com.payment.reporting.dto.TransactionReport;
import com.payment.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportingService service;
    
    @GetMapping("/transactions/{merchantId}")
    public TransactionReport getTransactionReport(@PathVariable String merchantId) {
        return service.generateTransactionReport(merchantId);
    }
}
EOF

cat > "$BASE_DIR/reporting-service/src/main/resources/application.yml" << 'EOF'
server:
  port: 8087
spring:
  application:
    name: reporting-service
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
EOF

echo "=== Creating Notification Service ==="

cat > "$BASE_DIR/notification-service/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.payment</groupId><artifactId>payment-system</artifactId><version>1.0.0</version></parent>
    <artifactId>notification-service</artifactId>
    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-mail</artifactId></dependency>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-netflix-eureka-client</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
    </dependencies>
</project>
EOF

mkdir -p "$BASE_DIR/notification-service/src/main/java/com/payment/notification"/{service,controller,dto}
mkdir -p "$BASE_DIR/notification-service/src/main/resources"

cat > "$BASE_DIR/notification-service/src/main/java/com/payment/notification/NotificationServiceApplication.java" << 'EOF'
package com.payment.notification;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
EOF

cat > "$BASE_DIR/notification-service/src/main/java/com/payment/notification/dto/NotificationRequest.java" << 'EOF'
package com.payment.notification.dto;
import lombok.Data;

@Data
public class NotificationRequest {
    private String to;
    private String subject;
    private String message;
    private String type; // EMAIL, SMS, PUSH
}
EOF

cat > "$BASE_DIR/notification-service/src/main/java/com/payment/notification/service/NotificationService.java" << 'EOF'
package com.payment.notification.service;
import com.payment.notification.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    
    public void sendNotification(NotificationRequest request) {
        log.info("Sending {} notification to: {}", request.getType(), request.getTo());
        log.info("Subject: {}", request.getSubject());
        log.info("Message: {}", request.getMessage());
        // Implement actual notification logic (email, SMS, push)
    }
    
    public void sendTransactionAlert(String merchantId, String transactionId, String status) {
        log.info("Transaction alert - Merchant: {}, Transaction: {}, Status: {}", 
            merchantId, transactionId, status);
    }
}
EOF

cat > "$BASE_DIR/notification-service/src/main/java/com/payment/notification/controller/NotificationController.java" << 'EOF'
package com.payment.notification.controller;
import com.payment.notification.dto.NotificationRequest;
import com.payment.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;
    
    @PostMapping("/send")
    public void sendNotification(@RequestBody NotificationRequest request) {
        service.sendNotification(request);
    }
}
EOF

cat > "$BASE_DIR/notification-service/src/main/resources/application.yml" << 'EOF'
server:
  port: 8088
spring:
  application:
    name: notification-service
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
EOF

echo ""
echo "================================================"
echo "✅ ALL SERVICES CREATED SUCCESSFULLY!"
echo "================================================"
echo ""
echo "Services created:"
echo "  1. Service Registry (Port 8761)"
echo "  2. Config Server (Port 8888)"
echo "  3. API Gateway (Port 8080)"
echo "  4. Merchant Service (Port 8081)"
echo "  5. Acquirer Service (Port 8082)"
echo "  6. Issuer Service (Port 8083)"
echo "  7. Network Service (Port 8084)"
echo "  8. Transaction Service (Port 8085)"
echo "  9. Settlement Service (Port 8086)"
echo "  10. Reporting Service (Port 8087)"
echo "  11. Notification Service (Port 8088)"
echo ""
echo "Next steps:"
echo "  1. Build all services: mvn clean install"
echo "  2. Start services: ./start-all.sh"
echo ""
