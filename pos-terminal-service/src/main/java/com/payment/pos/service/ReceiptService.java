package com.payment.pos.service;

import com.payment.common.dto.CardData;
import com.payment.pos.model.POSTransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ReceiptService {
    
    public String generateReceipt(POSTransactionRequest request, CardData cardData,
                                 boolean approved, String responseCode, 
                                 String responseMessage, String authCode) {
        StringBuilder receipt = new StringBuilder();
        
        receipt.append("\n");
        receipt.append("╔════════════════════════════════════╗\n");
        receipt.append("║     TRANSACTION RECEIPT            ║\n");
        receipt.append("╚════════════════════════════════════╝\n");
        receipt.append("\n");
        receipt.append(String.format("%-20s %s\n", "Merchant:", request.getMerchantName()));
        receipt.append(String.format("%-20s %s\n", "Terminal:", request.getTerminalId()));
        receipt.append("\n");
        receipt.append("────────────────────────────────────\n");
        receipt.append("\n");
        receipt.append(String.format("%-20s ****%s\n", "Card:", 
                cardData.getPan().substring(cardData.getPan().length() - 4)));
        receipt.append(String.format("%-20s %s\n", "Type:", cardData.getCardType()));
        receipt.append(String.format("%-20s %s\n", "Entry:", 
                request.getCardReadMethod().toString()));
        receipt.append("\n");
        receipt.append("────────────────────────────────────\n");
        receipt.append("\n");
        receipt.append(String.format("%-20s $%,.2f\n", "Amount:", request.getAmount() / 100.0));
        receipt.append("\n");
        receipt.append("────────────────────────────────────\n");
        receipt.append("\n");
        
        if (approved) {
            receipt.append(String.format("%-20s ✓ APPROVED\n", "Status:"));
            receipt.append(String.format("%-20s %s\n", "Auth Code:", authCode));
        } else {
            receipt.append(String.format("%-20s ✗ DECLINED\n", "Status:"));
            receipt.append(String.format("%-20s %s\n", "Response:", responseCode));
            receipt.append(String.format("%-20s %s\n", "Reason:", responseMessage));
        }
        
        receipt.append("\n");
        receipt.append(String.format("%-20s %s\n", "Date/Time:", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        receipt.append("\n");
        receipt.append("────────────────────────────────────\n");
        receipt.append("   Thank you for your business!\n");
        receipt.append("════════════════════════════════════\n");
        
        return receipt.toString();
    }
}