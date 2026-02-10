#!/bin/bash

# Complete All Remaining Services for POS Payment System
BASE_DIR="/home/claude/payment-system"
cd "$BASE_DIR"

echo "=== Creating Complete Issuer Service ==="

cat > "$BASE_DIR/issuer-service/pom.xml" << 'POMEOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.payment</groupId>
        <artifactId>payment-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>issuer-service</artifactId>
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
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.2.0</version>
        </dependency>
    </dependencies>
</project>
POMEOF

mkdir -p "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer"/{model,repository,service,controller,dto}
mkdir -p "$BASE_DIR/issuer-service/src/main/resources"

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/IssuerServiceApplication.java" << 'JAVAEOF'
package com.payment.issuer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class IssuerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IssuerServiceApplication.class, args);
    }
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/model/Card.java" << 'JAVAEOF'
package com.payment.issuer.model;

import com.payment.common.enums.CardType;
import com.payment.common.enums.PaymentNetwork;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String cardNumber;
    private String cardholderName;
    private String cvv;
    private LocalDate expiryDate;
    
    @Enumerated(EnumType.STRING)
    private CardType cardType;
    
    @Enumerated(EnumType.STRING)
    private PaymentNetwork network;
    
    private String accountId;
    private BigDecimal creditLimit;
    private BigDecimal availableBalance;
    private boolean active;
    private boolean blocked;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastUsed;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
        blocked = false;
        if (cardNumber == null) {
            cardNumber = generateCardNumber();
        }
        if (cvv == null) {
            cvv = generateCVV();
        }
        if (expiryDate == null) {
            expiryDate = LocalDate.now().plusYears(3);
        }
        if (availableBalance == null) {
            availableBalance = creditLimit != null ? creditLimit : BigDecimal.ZERO;
        }
    }
    
    private String generateCardNumber() {
        return "4" + String.format("%015d", (long)(Math.random() * 1000000000000000L));
    }
    
    private String generateCVV() {
        return String.format("%03d", (int)(Math.random() * 1000));
    }
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/model/CardAccount.java" << 'JAVAEOF'
package com.payment.issuer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_accounts")
@Data
@NoArgsConstructor
public class CardAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String customerId;
    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (accountNumber == null) {
            accountNumber = "ACC" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/dto/CardRequest.java" << 'JAVAEOF'
package com.payment.issuer.dto;

import com.payment.common.enums.CardType;
import com.payment.common.enums.PaymentNetwork;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardRequest {
    @NotBlank(message = "Cardholder name is required")
    private String cardholderName;
    
    @NotNull(message = "Card type is required")
    private CardType cardType;
    
    private PaymentNetwork network = PaymentNetwork.VISA;
    private String accountId;
    private BigDecimal creditLimit;
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/dto/CardResponse.java" << 'JAVAEOF'
package com.payment.issuer.dto;

import com.payment.common.enums.CardType;
import com.payment.common.enums.PaymentNetwork;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CardResponse {
    private String id;
    private String cardNumber;
    private String cardholderName;
    private LocalDate expiryDate;
    private CardType cardType;
    private PaymentNetwork network;
    private BigDecimal creditLimit;
    private BigDecimal availableBalance;
    private boolean active;
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/dto/AuthorizationRequest.java" << 'JAVAEOF'
package com.payment.issuer.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AuthorizationRequest {
    private String cardNumber;
    private String cvv;
    private String expiryDate;
    private BigDecimal amount;
    private String currency;
    private String merchantId;
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/dto/AuthorizationResponse.java" << 'JAVAEOF'
package com.payment.issuer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizationResponse {
    private boolean approved;
    private String authorizationCode;
    private String responseCode;
    private String message;
    private String transactionId;
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/repository/CardRepository.java" << 'JAVAEOF'
package com.payment.issuer.repository;

import com.payment.issuer.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    Optional<Card> findByCardNumber(String cardNumber);
    boolean existsByCardNumber(String cardNumber);
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/repository/CardAccountRepository.java" << 'JAVAEOF'
package com.payment.issuer.repository;

import com.payment.issuer.model.CardAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardAccountRepository extends JpaRepository<CardAccount, String> {
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/service/CardService.java" << 'JAVAEOF'
package com.payment.issuer.service;

import com.payment.issuer.dto.*;
import com.payment.issuer.model.Card;
import com.payment.issuer.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {
    private final CardRepository cardRepository;
    
    @Transactional
    public CardResponse issueCard(CardRequest request) {
        log.info("Issuing new card for: {}", request.getCardholderName());
        
        Card card = new Card();
        card.setCardholderName(request.getCardholderName());
        card.setCardType(request.getCardType());
        card.setNetwork(request.getNetwork());
        card.setAccountId(request.getAccountId());
        card.setCreditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : new BigDecimal("5000"));
        
        Card saved = cardRepository.save(card);
        log.info("Card issued: {}", saved.getCardNumber());
        
        return toResponse(saved);
    }
    
    public CardResponse getCard(String id) {
        Card card = cardRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Card not found: " + id));
        return toResponse(card);
    }
    
    public List<CardResponse> getAllCards() {
        return cardRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public AuthorizationResponse authorizeTransaction(AuthorizationRequest request) {
        log.info("Authorizing transaction for card: {}", maskCardNumber(request.getCardNumber()));
        
        Card card = cardRepository.findByCardNumber(request.getCardNumber())
            .orElseThrow(() -> new RuntimeException("Card not found"));
        
        // Validate card
        if (!card.isActive()) {
            return buildDeclinedResponse("Card is not active");
        }
        
        if (card.isBlocked()) {
            return buildDeclinedResponse("Card is blocked");
        }
        
        // Validate CVV
        if (!card.getCvv().equals(request.getCvv())) {
            return buildDeclinedResponse("Invalid CVV");
        }
        
        // Check balance/limit
        if (card.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            return buildDeclinedResponse("Insufficient funds");
        }
        
        // Approve and hold funds
        card.setAvailableBalance(card.getAvailableBalance().subtract(request.getAmount()));
        cardRepository.save(card);
        
        String authCode = generateAuthCode();
        log.info("Transaction authorized: {}", authCode);
        
        return AuthorizationResponse.builder()
            .approved(true)
            .authorizationCode(authCode)
            .responseCode("00")
            .message("Approved")
            .transactionId(UUID.randomUUID().toString())
            .build();
    }
    
    @Transactional
    public void blockCard(String cardId) {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found"));
        card.setBlocked(true);
        cardRepository.save(card);
        log.info("Card blocked: {}", cardId);
    }
    
    private CardResponse toResponse(Card card) {
        return CardResponse.builder()
            .id(card.getId())
            .cardNumber(maskCardNumber(card.getCardNumber()))
            .cardholderName(card.getCardholderName())
            .expiryDate(card.getExpiryDate())
            .cardType(card.getCardType())
            .network(card.getNetwork())
            .creditLimit(card.getCreditLimit())
            .availableBalance(card.getAvailableBalance())
            .active(card.isActive())
            .build();
    }
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 16) return cardNumber;
        return cardNumber.substring(0, 4) + "********" + cardNumber.substring(12);
    }
    
    private AuthorizationResponse buildDeclinedResponse(String reason) {
        return AuthorizationResponse.builder()
            .approved(false)
            .authorizationCode(null)
            .responseCode("05")
            .message(reason)
            .transactionId(null)
            .build();
    }
    
    private String generateAuthCode() {
        return String.format("%06d", (int)(Math.random() * 1000000));
    }
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/java/com/payment/issuer/controller/CardController.java" << 'JAVAEOF'
package com.payment.issuer.controller;

import com.payment.issuer.dto.*;
import com.payment.issuer.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Card Issuer", description = "Card issuance and authorization APIs")
public class CardController {
    private final CardService cardService;
    
    @PostMapping
    @Operation(summary = "Issue a new card")
    public ResponseEntity<CardResponse> issueCard(@Valid @RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(cardService.issueCard(request));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get card details")
    public ResponseEntity<CardResponse> getCard(@PathVariable String id) {
        return ResponseEntity.ok(cardService.getCard(id));
    }
    
    @GetMapping
    @Operation(summary = "Get all cards")
    public ResponseEntity<List<CardResponse>> getAllCards() {
        return ResponseEntity.ok(cardService.getAllCards());
    }
    
    @PostMapping("/authorize")
    @Operation(summary = "Authorize a transaction")
    public ResponseEntity<AuthorizationResponse> authorize(@Valid @RequestBody AuthorizationRequest request) {
        return ResponseEntity.ok(cardService.authorizeTransaction(request));
    }
    
    @PutMapping("/{id}/block")
    @Operation(summary = "Block a card")
    public ResponseEntity<Void> blockCard(@PathVariable String id) {
        cardService.blockCard(id);
        return ResponseEntity.ok().build();
    }
}
JAVAEOF

cat > "$BASE_DIR/issuer-service/src/main/resources/application.yml" << 'YMLEOF'
server:
  port: 8083

spring:
  application:
    name: issuer-service
  datasource:
    url: jdbc:h2:mem:issuerdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: "*"
YMLEOF

echo "Issuer Service completed!"

# Now create Transaction Service
echo "=== Creating Transaction Service ==="

cat > "$BASE_DIR/transaction-service/pom.xml" << 'POMEOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.payment</groupId>
        <artifactId>payment-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>transaction-service</artifactId>
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
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.2.0</version>
        </dependency>
    </dependencies>
</project>
POMEOF

mkdir -p "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction"/{model,repository,service,controller,dto,client}
mkdir -p "$BASE_DIR/transaction-service/src/main/resources"

cat > "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction/TransactionServiceApplication.java" << 'JAVAEOF'
package com.payment.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class TransactionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
JAVAEOF

cat > "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction/model/Transaction.java" << 'JAVAEOF'
package com.payment.transaction.model;

import com.payment.common.enums.TransactionStatus;
import com.payment.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String merchantId;
    private String cardNumber;
    private String terminalId;
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    private BigDecimal amount;
    private String currency;
    
    private String authorizationCode;
    private String responseCode;
    private String responseMessage;
    
    private String acquirerId;
    private String issuerId;
    private String networkId;
    
    private LocalDateTime createdAt;
    private LocalDateTime authorizedAt;
    private LocalDateTime settledAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = TransactionStatus.INITIATED;
        }
        if (currency == null) {
            currency = "USD";
        }
    }
}
JAVAEOF

cat > "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction/dto/TransactionRequest.java" << 'JAVAEOF'
package com.payment.transaction.dto;

import com.payment.common.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotBlank(message = "Merchant ID is required")
    private String merchantId;
    
    @NotBlank(message = "Card number is required")
    private String cardNumber;
    
    @NotBlank(message = "CVV is required")
    private String cvv;
    
    private String terminalId;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType type;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String currency = "USD";
}
JAVAEOF

cat > "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction/dto/TransactionResponse.java" << 'JAVAEOF'
package com.payment.transaction.dto;

import com.payment.common.enums.TransactionStatus;
import com.payment.common.enums.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private String id;
    private String merchantId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String authorizationCode;
    private String responseMessage;
    private LocalDateTime createdAt;
}
JAVAEOF

cat > "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction/repository/TransactionRepository.java" << 'JAVAEOF'
package com.payment.transaction.repository;

import com.payment.transaction.model.Transaction;
import com.payment.common.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByMerchantId(String merchantId);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
JAVAEOF

cat > "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction/client/IssuerClient.java" << 'JAVAEOF'
package com.payment.transaction.client;

import com.payment.issuer.dto.AuthorizationRequest;
import com.payment.issuer.dto.AuthorizationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "issuer-service", path = "/api/cards")
public interface IssuerClient {
    @PostMapping("/authorize")
    AuthorizationResponse authorize(@RequestBody AuthorizationRequest request);
}
JAVAEOF

cat > "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction/service/TransactionProcessingService.java" << 'JAVAEOF'
package com.payment.transaction.service;

import com.payment.transaction.client.IssuerClient;
import com.payment.transaction.dto.TransactionRequest;
import com.payment.transaction.dto.TransactionResponse;
import com.payment.transaction.model.Transaction;
import com.payment.transaction.repository.TransactionRepository;
import com.payment.issuer.dto.AuthorizationRequest;
import com.payment.issuer.dto.AuthorizationResponse;
import com.payment.common.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessingService {
    private final TransactionRepository transactionRepository;
    private final IssuerClient issuerClient;
    
    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.info("Processing transaction for merchant: {}", request.getMerchantId());
        
        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setMerchantId(request.getMerchantId());
        transaction.setCardNumber(request.getCardNumber());
        transaction.setTerminalId(request.getTerminalId());
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING);
        
        transaction = transactionRepository.save(transaction);
        
        try {
            // Call issuer for authorization
            AuthorizationRequest authRequest = new AuthorizationRequest();
            authRequest.setCardNumber(request.getCardNumber());
            authRequest.setCvv(request.getCvv());
            authRequest.setAmount(request.getAmount());
            authRequest.setCurrency(request.getCurrency());
            authRequest.setMerchantId(request.getMerchantId());
            
            AuthorizationResponse authResponse = issuerClient.authorize(authRequest);
            
            // Update transaction with response
            if (authResponse.isApproved()) {
                transaction.setStatus(TransactionStatus.AUTHORIZED);
                transaction.setAuthorizationCode(authResponse.getAuthorizationCode());
                transaction.setAuthorizedAt(LocalDateTime.now());
                log.info("Transaction authorized: {}", transaction.getId());
            } else {
                transaction.setStatus(TransactionStatus.DECLINED);
                log.warn("Transaction declined: {}", authResponse.getMessage());
            }
            
            transaction.setResponseCode(authResponse.getResponseCode());
            transaction.setResponseMessage(authResponse.getMessage());
            
        } catch (Exception e) {
            log.error("Transaction failed: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setResponseMessage("System error: " + e.getMessage());
        }
        
        transaction = transactionRepository.save(transaction);
        return toResponse(transaction);
    }
    
    public TransactionResponse getTransaction(String id) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
        return toResponse(transaction);
    }
    
    public List<TransactionResponse> getMerchantTransactions(String merchantId) {
        return transactionRepository.findByMerchantId(merchantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .merchantId(transaction.getMerchantId())
            .type(transaction.getType())
            .status(transaction.getStatus())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .authorizationCode(transaction.getAuthorizationCode())
            .responseMessage(transaction.getResponseMessage())
            .createdAt(transaction.getCreatedAt())
            .build();
    }
}
JAVAEOF

cat > "$BASE_DIR/transaction-service/src/main/java/com/payment/transaction/controller/TransactionController.java" << 'JAVAEOF'
package com.payment.transaction.controller;

import com.payment.transaction.dto.TransactionRequest;
import com.payment.transaction.dto.TransactionResponse;
import com.payment.transaction.service.TransactionProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Processing", description = "Transaction processing APIs")
public class TransactionController {
    private final TransactionProcessingService transactionService;
    
    @PostMapping("/authorize")
    @Operation(summary = "Process a transaction")
    public ResponseEntity<TransactionResponse> processTransaction(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(transactionService.processTransaction(request));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }
    
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get merchant transactions")
    public ResponseEntity<List<TransactionResponse>> getMerchantTransactions(@PathVariable String merchantId) {
        return ResponseEntity.ok(transactionService.getMerchantTransactions(merchantId));
    }
    
    @GetMapping
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }
}
JAVAEOF

cat > "$BASE_DIR/transaction-service/src/main/resources/application.yml" << 'YMLEOF'
server:
  port: 8085

spring:
  application:
    name: transaction-service
  datasource:
    url: jdbc:h2:mem:transactiondb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: "*"
YMLEOF

echo "Transaction Service completed!"
echo "All major services created successfully!"
