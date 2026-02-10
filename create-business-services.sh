#!/bin/bash

# Complete Business Services Generator for POS Payment System
BASE_DIR="/home/claude/payment-system"
cd "$BASE_DIR"

echo "Creating all business services with complete functionality..."

# API GATEWAY
cat > "$BASE_DIR/api-gateway/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.payment</groupId>
        <artifactId>payment-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>api-gateway</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
</project>
EOF

cat > "$BASE_DIR/api-gateway/src/main/java/com/payment/gateway/ApiGatewayApplication.java" << 'EOF'
package com.payment.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
EOF

cat > "$BASE_DIR/api-gateway/src/main/resources/application.yml" << 'EOF'
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: merchant-service
          uri: lb://merchant-service
          predicates:
            - Path=/api/merchants/**
        - id: acquirer-service
          uri: lb://acquirer-service
          predicates:
            - Path=/api/acquirers/**
        - id: issuer-service
          uri: lb://issuer-service
          predicates:
            - Path=/api/issuers/**,/api/cards/**
        - id: network-service
          uri: lb://network-service
          predicates:
            - Path=/api/networks/**
        - id: transaction-service
          uri: lb://transaction-service
          predicates:
            - Path=/api/transactions/**
        - id: settlement-service
          uri: lb://settlement-service
          predicates:
            - Path=/api/settlements/**
        - id: reporting-service
          uri: lb://reporting-service
          predicates:
            - Path=/api/reports/**
        - id: notification-service
          uri: lb://notification-service
          predicates:
            - Path=/api/notifications/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
EOF

echo "API Gateway created!"

# Now create complete MERCHANT SERVICE
cat > "$BASE_DIR/merchant-service/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.payment</groupId>
        <artifactId>payment-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>merchant-service</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.payment</groupId>
            <artifactId>common</artifactId>
            <version>1.0.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.2.0</version>
        </dependency>
    </dependencies>
</project>
EOF

mkdir -p "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant"/{model,repository,service,controller,dto,config}

cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/MerchantServiceApplication.java" << 'EOF'
package com.payment.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MerchantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MerchantServiceApplication.class, args);
    }
}
EOF

cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/model/Merchant.java" << 'EOF'
package com.payment.merchant.model;

import com.payment.common.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String businessName;
    private String email;
    private String phone;
    private String address;
    private String taxId;
    
    @Enumerated(EnumType.STRING)
    private MerchantStatus status;
    
    private String merchantCategoryCode; // MCC
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal currentDailyVolume;
    private BigDecimal currentMonthlyVolume;
    
    private String accountNumber;
    private String routingNumber;
    private String bankName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastTransactionAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = MerchantStatus.PENDING_VERIFICATION;
        }
        if (currentDailyVolume == null) {
            currentDailyVolume = BigDecimal.ZERO;
        }
        if (currentMonthlyVolume == null) {
            currentMonthlyVolume = BigDecimal.ZERO;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
EOF

cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/model/Terminal.java" << 'EOF'
package com.payment.merchant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "terminals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Terminal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String merchantId;
    private String terminalId;
    private String serialNumber;
    private String model;
    private String location;
    private boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
    }
}
EOF

cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/dto/MerchantRequest.java" << 'EOF'
package com.payment.merchant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MerchantRequest {
    @NotBlank(message = "Business name is required")
    private String businessName;
    
    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    private String taxId;
    private String merchantCategoryCode;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private String accountNumber;
    private String routingNumber;
    private String bankName;
}
EOF

cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/dto/MerchantResponse.java" << 'EOF'
package com.payment.merchant.dto;

import com.payment.common.enums.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponse {
    private String id;
    private String businessName;
    private String email;
    private String phone;
    private String address;
    private MerchantStatus status;
    private String merchantCategoryCode;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal currentDailyVolume;
    private BigDecimal currentMonthlyVolume;
    private LocalDateTime createdAt;
    private LocalDateTime lastTransactionAt;
}
EOF

cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/repository/MerchantRepository.java" << 'EOF'
package com.payment.merchant.repository;

import com.payment.merchant.model.Merchant;
import com.payment.common.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, String> {
    Optional<Merchant> findByEmail(String email);
    List<Merchant> findByStatus(MerchantStatus status);
    boolean existsByEmail(String email);
}
EOF

cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/repository/TerminalRepository.java" << 'EOF'
package com.payment.merchant.repository;

import com.payment.merchant.model.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, String> {
    List<Terminal> findByMerchantId(String merchantId);
    List<Terminal> findByMerchantIdAndActive(String merchantId, boolean active);
}
EOF

echo "Merchant Service models and repositories created!"

# Continue with service layer...
cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/service/MerchantService.java" << 'EOF'
package com.payment.merchant.service;

import com.payment.merchant.dto.MerchantRequest;
import com.payment.merchant.dto.MerchantResponse;
import com.payment.merchant.model.Merchant;
import com.payment.merchant.repository.MerchantRepository;
import com.payment.common.enums.MerchantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {
    private final MerchantRepository merchantRepository;
    
    @Transactional
    public MerchantResponse createMerchant(MerchantRequest request) {
        log.info("Creating merchant: {}", request.getBusinessName());
        
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Merchant with email already exists: " + request.getEmail());
        }
        
        Merchant merchant = new Merchant();
        merchant.setBusinessName(request.getBusinessName());
        merchant.setEmail(request.getEmail());
        merchant.setPhone(request.getPhone());
        merchant.setAddress(request.getAddress());
        merchant.setTaxId(request.getTaxId());
        merchant.setMerchantCategoryCode(request.getMerchantCategoryCode());
        merchant.setDailyLimit(request.getDailyLimit() != null ? request.getDailyLimit() : new BigDecimal("10000"));
        merchant.setMonthlyLimit(request.getMonthlyLimit() != null ? request.getMonthlyLimit() : new BigDecimal("300000"));
        merchant.setAccountNumber(request.getAccountNumber());
        merchant.setRoutingNumber(request.getRoutingNumber());
        merchant.setBankName(request.getBankName());
        merchant.setStatus(MerchantStatus.PENDING_VERIFICATION);
        
        Merchant saved = merchantRepository.save(merchant);
        log.info("Merchant created with ID: {}", saved.getId());
        
        return toResponse(saved);
    }
    
    public MerchantResponse getMerchant(String id) {
        Merchant merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Merchant not found: " + id));
        return toResponse(merchant);
    }
    
    public List<MerchantResponse> getAllMerchants() {
        return merchantRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public MerchantResponse updateMerchantStatus(String id, MerchantStatus status) {
        Merchant merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Merchant not found: " + id));
        merchant.setStatus(status);
        return toResponse(merchantRepository.save(merchant));
    }
    
    @Transactional
    public void updateVolume(String merchantId, BigDecimal amount) {
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new RuntimeException("Merchant not found: " + merchantId));
        
        merchant.setCurrentDailyVolume(merchant.getCurrentDailyVolume().add(amount));
        merchant.setCurrentMonthlyVolume(merchant.getCurrentMonthlyVolume().add(amount));
        merchantRepository.save(merchant);
    }
    
    private MerchantResponse toResponse(Merchant merchant) {
        return MerchantResponse.builder()
            .id(merchant.getId())
            .businessName(merchant.getBusinessName())
            .email(merchant.getEmail())
            .phone(merchant.getPhone())
            .address(merchant.getAddress())
            .status(merchant.getStatus())
            .merchantCategoryCode(merchant.getMerchantCategoryCode())
            .dailyLimit(merchant.getDailyLimit())
            .monthlyLimit(merchant.getMonthlyLimit())
            .currentDailyVolume(merchant.getCurrentDailyVolume())
            .currentMonthlyVolume(merchant.getCurrentMonthlyVolume())
            .createdAt(merchant.getCreatedAt())
            .lastTransactionAt(merchant.getLastTransactionAt())
            .build();
    }
}
EOF

cat > "$BASE_DIR/merchant-service/src/main/java/com/payment/merchant/controller/MerchantController.java" << 'EOF'
package com.payment.merchant.controller;

import com.payment.merchant.dto.MerchantRequest;
import com.payment.merchant.dto.MerchantResponse;
import com.payment.merchant.service.MerchantService;
import com.payment.common.enums.MerchantStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchant Management", description = "APIs for merchant operations")
public class MerchantController {
    private final MerchantService merchantService;
    
    @PostMapping
    @Operation(summary = "Create new merchant")
    public ResponseEntity<MerchantResponse> createMerchant(@Valid @RequestBody MerchantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(merchantService.createMerchant(request));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get merchant by ID")
    public ResponseEntity<MerchantResponse> getMerchant(@PathVariable String id) {
        return ResponseEntity.ok(merchantService.getMerchant(id));
    }
    
    @GetMapping
    @Operation(summary = "Get all merchants")
    public ResponseEntity<List<MerchantResponse>> getAllMerchants() {
        return ResponseEntity.ok(merchantService.getAllMerchants());
    }
    
    @PutMapping("/{id}/status")
    @Operation(summary = "Update merchant status")
    public ResponseEntity<MerchantResponse> updateStatus(
            @PathVariable String id,
            @RequestParam MerchantStatus status) {
        return ResponseEntity.ok(merchantService.updateMerchantStatus(id, status));
    }
}
EOF

cat > "$BASE_DIR/merchant-service/src/main/resources/application.yml" << 'EOF'
server:
  port: 8081

spring:
  application:
    name: merchant-service
  datasource:
    url: jdbc:h2:mem:merchantdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: "*"
EOF

echo "Merchant Service completed!"
echo "Script execution finished. All basic structures created."
echo "Run: cd /home/claude/payment-system && find . -name '*.java' | wc -l to see created files"
