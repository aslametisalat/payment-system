package com.payment.pos.controller;

import com.payment.pos.model.POSTransactionRequest;
import com.payment.pos.model.POSTransactionResult;
import com.payment.pos.service.POSTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "POS Terminal", description = "POS Terminal with ISO 8583 and full security")
public class POSController {
    
    private final POSTransactionService posService;
    
    @PostMapping("/transaction")
    @Operation(summary = "Process POS transaction with full security (PIN, MAC, EMV)")
    public ResponseEntity<POSTransactionResult> processTransaction(
            @Valid @RequestBody POSTransactionRequest request) {
        
        log.info("╔═══════════════════════════════════════════════════════╗");
        log.info("║  POS Transaction Request Received                     ║");
        log.info("╠═══════════════════════════════════════════════════════╣");
        log.info("║  Terminal: {}", String.format("%-40s", request.getTerminalId()) + "║");
        log.info("║  Merchant: {}", String.format("%-40s", request.getMerchantId()) + "║");
        log.info("║  Amount:   ${}",  String.format("%-38.2f", request.getAmount() / 100.0) + "║");
        log.info("║  Method:   {}", String.format("%-40s", request.getCardReadMethod()) + "║");
        log.info("╚═══════════════════════════════════════════════════════╝");
        
        POSTransactionResult result = posService.processTransaction(request);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Check POS terminal health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("POS Terminal Operational");
    }
}