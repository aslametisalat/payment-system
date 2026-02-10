package com.payment.reporting.controller;

import com.payment.reporting.dto.TransactionReport;
import com.payment.reporting.dto.MerchantDashboard;
import com.payment.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reporting", description = "Analytics and Reporting Operations")
public class ReportingController {
    
    private final ReportingService reportingService;
    
    /**
     * Get transaction report for merchant
     */
    @GetMapping("/transactions/{merchantId}")
    @Operation(summary = "Get transaction report", 
               description = "Get aggregated transaction report for merchant")
    public ResponseEntity<TransactionReport> getTransactionReport(
            @PathVariable String merchantId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("╔═══════════════════════════════════════════════╗");
        log.info("║  GENERATING TRANSACTION REPORT                ║");
        log.info("╠═══════════════════════════════════════════════╣");
        log.info("║  Merchant: {}", String.format("%-33s", merchantId) + "║");
        if (startDate != null) {
            log.info("║  Start Date: {}", String.format("%-31s", startDate) + "║");
        }
        if (endDate != null) {
            log.info("║  End Date: {}", String.format("%-33s", endDate) + "║");
        }
        log.info("╚═══════════════════════════════════════════════╝");
        
        // TransactionReport report = reportingService.generateTransactionReport(
        //     merchantId, startDate, endDate
        // );
        TransactionReport report = reportingService.generateTransactionReport(merchantId);
        
        log.info("Report generated:");
        log.info("  Total Transactions: {}", report.getTotalTransactions());
        log.info("  Total Volume: ${}", report.getTotalVolume());
        log.info("  Approval Rate: {}%", report.getApprovalRate());
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get merchant dashboard summary
     */
    @GetMapping("/dashboard/{merchantId}")
    @Operation(summary = "Get merchant dashboard", 
               description = "Get comprehensive dashboard data for merchant")
    public ResponseEntity<MerchantDashboard> getMerchantDashboard(
            @PathVariable String merchantId) {
        
        log.info("Generating dashboard for merchant: {}", merchantId);
        // MerchantDashboard dashboard = reportingService.getMerchantDashboard(merchantId);
        // return ResponseEntity.ok(dashboard);
        return ResponseEntity.notFound().build(); // Method not implemented
    }
    
    /**
     * Get daily summary
     */
    @GetMapping("/daily/{merchantId}")
    @Operation(summary = "Get daily summary")
    public ResponseEntity<TransactionReport> getDailySummary(
            @PathVariable String merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Getting daily summary for {} on {}", merchantId, date);
        TransactionReport report = reportingService.generateTransactionReport(merchantId);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get monthly summary
     */
    @GetMapping("/monthly/{merchantId}/{year}/{month}")
    @Operation(summary = "Get monthly summary")
    public ResponseEntity<TransactionReport> getMonthlySummary(
            @PathVariable String merchantId,
            @PathVariable int year,
            @PathVariable int month) {
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        log.info("Getting monthly summary for {}: {}/{}", merchantId, month, year);
        TransactionReport report = reportingService.generateTransactionReport(merchantId);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Reporting Service Operational");
    }
}