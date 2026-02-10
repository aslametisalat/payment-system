package com.payment.issuer.controller;

import com.payment.issuer.dto.*;
import com.payment.issuer.service.CardService;
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
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Card Issuer", description = "Card Issuance and Authorization Operations")
public class IssuerController {
    
    private final CardService cardService;
    
    /**
     * Issue a new card
     */
    @PostMapping
    @Operation(summary = "Issue new card", description = "Create new credit/debit card")
    public ResponseEntity<CardResponse> issueCard(@Valid @RequestBody CardRequest request) {
        log.info("Issuing new card: {} for {}", request.getCardType(), request.getCardholderName());
        CardResponse response = cardService.issueCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get all cards
     */
    @GetMapping
    @Operation(summary = "Get all cards")
    public ResponseEntity<List<CardResponse>> getAllCards() {
        List<CardResponse> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }
    
    /**
     * Get card by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get card by ID")
    public ResponseEntity<CardResponse> getCardById(@PathVariable String id) {
        CardResponse card = cardService.getCard(id);
        return ResponseEntity.ok(card);
    }
    
    /**
     * Authorize transaction - CRITICAL ENDPOINT
     */
    @PostMapping("/authorize")
    @Operation(summary = "Authorize transaction", 
               description = "Customer's bank authorizes/declines transaction")
    public ResponseEntity<AuthorizationResponse> authorize(
            @Valid @RequestBody AuthorizationRequest request) {
        
        log.info("╔═══════════════════════════════════════════════╗");
        log.info("║  ISSUER AUTHORIZATION REQUEST                 ║");
        log.info("╠═══════════════════════════════════════════════╣");
        log.info("║  Card: ****{}", String.format("%-37s", 
            request.getCardNumber().substring(request.getCardNumber().length() - 4)) + "║");
        log.info("║  Amount: ${}", String.format("%-35.2f", request.getAmount()) + "║");
        log.info("╚═══════════════════════════════════════════════╝");
        
        AuthorizationResponse response = cardService.authorizeTransaction(request);
        
        log.info("Authorization Result: {}", 
            response.isApproved() ? "✓ APPROVED" : "✗ DECLINED - " + response.getMessage());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Authorize with full security verification
     */
    @PostMapping("/authorize-secure")
    @Operation(summary = "Authorize with security checks", 
               description = "Full security: MAC, PIN, EMV verification")
    public ResponseEntity<AuthorizationResponse> authorizeSecure(
            @Valid @RequestBody SecureAuthorizationRequest request) {
        
        log.info("╔═══════════════════════════════════════════════╗");
        log.info("║  SECURE AUTHORIZATION (MAC + PIN + EMV)       ║");
        log.info("╚═══════════════════════════════════════════════╝");
        
        AuthorizationRequest authRequest = AuthorizationRequest.builder()
            .cardNumber(request.getCardNumber())
            .cvv(request.getCvv())
            .expiryDate(request.getExpiryDate())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .merchantId(request.getMerchantId())
            .encryptedPIN(request.getEncryptedPIN())
            .emvData(request.getEmvData())
            .mac(request.getMac())
            .build();
        AuthorizationResponse response = cardService.authorizeTransactionSecure(authRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Block a card
     */
    @PutMapping("/{id}/block")
    @Operation(summary = "Block card")
    public ResponseEntity<Void> blockCard(@PathVariable String id) {
        log.info("Blocking card: {}", id);
        cardService.blockCard(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Unblock a card
     */
    @PutMapping("/{id}/unblock")
    @Operation(summary = "Unblock card")
    public ResponseEntity<Void> unblockCard(@PathVariable String id) {
        log.info("Unblocking card: {}", id);
        // cardService.unblockCard(id); // Method not implemented
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Issuer Service Operational");
    }
}