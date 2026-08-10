package com.payment.merchant.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.payment.common.enums.MerchantStatus;
import com.payment.merchant.dto.MerchantRequest;
import com.payment.merchant.dto.MerchantResponse;
import com.payment.merchant.service.MerchantService;

import java.math.BigDecimal;
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
        return ResponseEntity.ok(merchantService.getMerchantById(id));
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

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate merchant for a transaction",
               description = "Checks merchant status and daily/monthly volume limits before authorizing a transaction")
    public ResponseEntity<MerchantService.ValidationResult> validateForTransaction(
            @PathVariable String id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(merchantService.validateForTransaction(id, amount));
    }
}
