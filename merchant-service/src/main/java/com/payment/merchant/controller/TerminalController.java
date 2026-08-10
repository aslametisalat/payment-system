package com.payment.merchant.controller;

import com.payment.merchant.dto.TerminalRequest;
import com.payment.merchant.dto.TerminalResponse;
import com.payment.merchant.service.TerminalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terminals")
@RequiredArgsConstructor
@Tag(name = "POS Terminal Management", description = "APIs for merchant POS terminal registration")
public class TerminalController {

    private final TerminalService terminalService;

    @PostMapping
    @Operation(summary = "Register a new POS terminal for a merchant")
    public ResponseEntity<TerminalResponse> registerTerminal(@Valid @RequestBody TerminalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(terminalService.registerTerminal(request));
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get all terminals for a merchant")
    public ResponseEntity<List<TerminalResponse>> getTerminalsByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(terminalService.getTerminalsByMerchant(merchantId));
    }

    @GetMapping("/{terminalId}")
    @Operation(summary = "Get terminal by terminal ID")
    public ResponseEntity<TerminalResponse> getTerminal(@PathVariable String terminalId) {
        return ResponseEntity.ok(terminalService.getTerminalByTerminalId(terminalId));
    }

    @PutMapping("/{terminalId}/deactivate")
    @Operation(summary = "Deactivate a terminal")
    public ResponseEntity<TerminalResponse> deactivateTerminal(@PathVariable String terminalId) {
        return ResponseEntity.ok(terminalService.deactivateTerminal(terminalId));
    }
}
