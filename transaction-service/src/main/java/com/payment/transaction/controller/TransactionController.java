package com.payment.transaction.controller;

import com.payment.transaction.dto.*;
import com.payment.transaction.service.TransactionProcessingService;
import com.payment.transaction.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Transaction Processing and Management")
public class TransactionController {
    
    private final TransactionProcessingService transactionService;
    
    /**
     * Process authorization (main endpoint)
     */
    @PostMapping("/authorize")
    @Operation(summary = "Authorize transaction", 
               description = "Complete transaction authorization flow")
    public ResponseEntity<TransactionResponse> authorize(
            @Valid @RequestBody TransactionRequest request) {
        
        log.info("╔═══════════════════════════════════════════════╗");
        log.info("║  TRANSACTION AUTHORIZATION REQUEST            ║");
        log.info("╠═══════════════════════════════════════════════╣");
        log.info("║  Merchant: {}", String.format("%-33s", request.getMerchantId()) + "║");
        log.info("║  Amount: ${}", String.format("%-35.2f", request.getAmount()) + "║");
        log.info("║  Card: ****{}", String.format("%-37s",
            request.getCardNumber().substring(request.getCardNumber().length() - 4)) + "║");
        log.info("╚═══════════════════════════════════════════════╝");
        
        TransactionResponse response = transactionService.processTransaction(request);
        
        HttpStatus status = response.getStatus() != null && 
            response.getStatus().toString().contains("AUTHORIZED") 
            ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Get all transactions
     */
    @GetMapping
    @Operation(summary = "Get all transactions")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        List<TransactionResponse> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Get transaction by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable String id) {
        TransactionResponse transaction = transactionService.getTransaction(id);
        return ResponseEntity.ok(transaction);
    }
    
    /**
     * Get merchant's transactions
     */
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get transactions for a merchant")
    public ResponseEntity<List<TransactionResponse>> getMerchantTransactions(
            @PathVariable String merchantId) {
        List<TransactionResponse> transactions = transactionService.getMerchantTransactions(merchantId);
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Refund a transaction
     */
    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund transaction")
    public ResponseEntity<Void> refundTransaction(@PathVariable String id) {
        log.info("Processing refund for transaction: {}", id);
        // TransactionResponse response = transactionService.refundTransaction(id);
        // return ResponseEntity.ok(response);
        return ResponseEntity.ok().build(); // Method not implemented
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service Operational");
    }
}