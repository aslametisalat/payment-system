package com.payment.transaction.service;

import com.payment.transaction.client.IssuerClient;
import com.payment.transaction.dto.AuthorizationRequest;
import com.payment.transaction.dto.AuthorizationResponse;
import com.payment.transaction.dto.TransactionRequest;
import com.payment.transaction.dto.TransactionResponse;
import com.payment.transaction.model.Transaction;
import com.payment.transaction.repository.TransactionRepository;
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
