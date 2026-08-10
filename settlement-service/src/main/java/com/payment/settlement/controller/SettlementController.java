package com.payment.settlement.controller;

import com.payment.settlement.dto.SettlementRequest;
import com.payment.settlement.dto.SettlementResponse;
import com.payment.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settlement", description = "Financial Settlement Operations")
public class SettlementController {
    
    private final SettlementService settlementService;
    
    /**
     * Get settlements for a merchant
     */
    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get merchant settlements", 
               description = "Get all settlements for a specific merchant")
    public ResponseEntity<List<SettlementResponse>> getMerchantSettlements(
            @PathVariable String merchantId) {
        
        log.info("Fetching settlements for merchant: {}", merchantId);
        List<SettlementResponse> settlements = settlementService.getMerchantSettlements(merchantId);
        return ResponseEntity.ok(settlements);
    }
    
    /**
     * Get settlement by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get settlement by ID")
    public ResponseEntity<SettlementResponse> getSettlementById(@PathVariable String id) {
        Optional<SettlementResponse> settlement = settlementService.getSettlementById(id);
        return settlement.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Get all settlements
     */
    @GetMapping
    @Operation(summary = "Get all settlements")
    public ResponseEntity<List<SettlementResponse>> getAllSettlements() {
        return ResponseEntity.ok(settlementService.getAllSettlements());
    }
    
    /**
     * Trigger manual settlement (for testing)
     */
    @PostMapping("/trigger")
    @Operation(summary = "Trigger settlement manually", 
               description = "Manually trigger settlement process (normally runs at 2 AM)")
    public ResponseEntity<String> triggerSettlement() {
        log.info("╔═══════════════════════════════════════════════╗");
        log.info("║  MANUAL SETTLEMENT TRIGGERED                  ║");
        log.info("╚═══════════════════════════════════════════════╝");
        
        settlementService.processSettlements();
        return ResponseEntity.ok("Settlement process initiated");
    }
    
    /**
     * Create settlement for specific merchant
     */
    @PostMapping("/create")
    @Operation(summary = "Create settlement", 
               description = "Create settlement for specific merchant and date")
    public ResponseEntity<SettlementResponse> createSettlement(
            @Valid @RequestBody SettlementRequest request) {

        log.info("Creating settlement for merchant: {}", request.getMerchantId());
        try {
            SettlementResponse response = settlementService.createSettlement(request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.warn("Settlement creation rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Settlement Service Operational");
    }
}