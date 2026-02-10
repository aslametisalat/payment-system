package com.payment.network.controller;

import com.payment.network.dto.RoutingRequest;
import com.payment.network.dto.RoutingResponse;
import com.payment.network.service.NetworkRoutingService;
import com.payment.common.enums.PaymentNetwork;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Network", description = "Payment Network Routing (Visa/Mastercard)")
public class NetworkController {
    
    private final NetworkRoutingService routingService;
    
    /**
     * Route transaction to correct issuer
     */
    @PostMapping("/route")
    @Operation(summary = "Route transaction", 
               description = "Determine card network and route to issuer bank")
    public ResponseEntity<RoutingResponse> routeTransaction(
            @Valid @RequestBody RoutingRequest request) {
        
        log.info("╔═══════════════════════════════════════════════╗");
        log.info("║  NETWORK ROUTING REQUEST                      ║");
        log.info("╠═══════════════════════════════════════════════╣");
        log.info("║  Card: ****{}", String.format("%-37s",
            request.getCardNumber().substring(request.getCardNumber().length() - 4)) + "║");
        log.info("║  Amount: ${}", String.format("%-35.2f", request.getAmount()) + "║");
        log.info("╚═══════════════════════════════════════════════╝");
        
        NetworkRoutingService.RoutingResult result = routingService.route(
            request.getCardNumber(),
            request.getAmount()
        );
        
        RoutingResponse response = RoutingResponse.builder()
            .network(result.getNetwork())
            .issuerId(result.getIssuerId())
            .networkFee(result.getNetworkFee())
            .build();
        
        log.info("Network: {}", result.getNetwork());
        log.info("Routing to issuer: {}", result.getIssuerId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get network info for a card
     */
    @GetMapping("/identify/{cardNumber}")
    @Operation(summary = "Identify card network")
    public ResponseEntity<PaymentNetwork> identifyNetwork(@PathVariable String cardNumber) {
        NetworkRoutingService.RoutingResult result = routingService.route(
            cardNumber, 
            java.math.BigDecimal.ZERO
        );
        return ResponseEntity.ok(result.getNetwork());
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Network Service Operational");
    }
}