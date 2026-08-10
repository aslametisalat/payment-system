package com.payment.acquirer.controller;

import com.payment.acquirer.dto.AcquirerRequest;
import com.payment.acquirer.dto.AcquirerResponse;
import com.payment.acquirer.service.AcquirerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/acquirers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Acquirer", description = "Acquirer (Merchant's Bank) Operations")
@CrossOrigin(origins = "*")
public class AcquirerController {
    
    private final AcquirerService acquirerService;
    
    /**
     * Process acquiring for a transaction
     * This represents the merchant's bank processing
     */
    @PostMapping("/process")
    @Operation(summary = "Process transaction acquiring", 
               description = "Merchant's bank processes transaction - fraud checks, routing")
    public ResponseEntity<AcquirerResponse> processAcquiring(
            @Valid @RequestBody AcquirerRequest request) {
        
        log.info("╔═══════════════════════════════════════════════╗");
        log.info("║  ACQUIRER PROCESSING REQUEST                  ║");
        log.info("╠═══════════════════════════════════════════════╣");
        log.info("║  Merchant: {}", String.format("%-33s", request.getMerchantId()) + "║");
        log.info("║  Amount: ${}", String.format("%-35.2f", request.getAmount()) + "║");
        log.info("╚═══════════════════════════════════════════════╝");
        
        AcquirerService.AcquiringResult result = acquirerService.processAcquiring(
            request.getMerchantId(),
            request.getCardNumber(),
            request.getAmount()
        );
        
        AcquirerResponse response = AcquirerResponse.builder()
            .approved(result.isApproved())
            .message(result.getMessage())
            .acquirerId("ACQ-123456")
            .fraudScore(result.getFraudScore())
            .build();
        
        log.info("Acquirer Result: {}", result.isApproved() ? "✓ APPROVED" : "✗ DECLINED");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Acquirer Service Operational");
    }
}