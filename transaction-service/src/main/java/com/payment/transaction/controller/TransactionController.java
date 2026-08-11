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

        // A decline (fraud, insufficient funds, limit exceeded, ...) is a
        // legitimate business outcome for a well-formed request, not a
        // client error - it comes back as 200 with status: DECLINED in the
        // body, same as real payment APIs model it. Callers like the POS
        // terminal need to be able to read the decline reason; a non-2xx
        // response here would make Feign throw instead of returning it.
        return ResponseEntity.ok(response);
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
    public ResponseEntity<TransactionResponse> refundTransaction(@PathVariable String id) {
        log.info("Processing refund for transaction: {}", id);
        try {
            TransactionResponse response = transactionService.refundTransaction(id);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("Refund rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service Operational");
    }
}