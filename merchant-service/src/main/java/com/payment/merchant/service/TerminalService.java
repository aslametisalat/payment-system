package com.payment.merchant.service;

import com.payment.merchant.dto.TerminalRequest;
import com.payment.merchant.dto.TerminalResponse;
import com.payment.merchant.model.Terminal;
import com.payment.merchant.repository.MerchantRepository;
import com.payment.merchant.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TerminalService {

    private final TerminalRepository terminalRepository;
    private final MerchantRepository merchantRepository;

    @Transactional
    public TerminalResponse registerTerminal(TerminalRequest request) {
        log.info("Registering terminal {} for merchant {}", request.getTerminalId(), request.getMerchantId());

        merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + request.getMerchantId()));

        if (terminalRepository.existsByTerminalId(request.getTerminalId())) {
            throw new RuntimeException("Terminal ID already registered: " + request.getTerminalId());
        }

        Terminal terminal = Terminal.builder()
                .merchantId(request.getMerchantId())
                .terminalId(request.getTerminalId())
                .serialNumber(request.getSerialNumber())
                .model(request.getModel())
                .location(request.getLocation())
                .active(true)
                .build();

        terminal = terminalRepository.save(terminal);
        log.info("Terminal registered with ID: {}", terminal.getId());

        return toResponse(terminal);
    }

    @Transactional(readOnly = true)
    public List<TerminalResponse> getTerminalsByMerchant(String merchantId) {
        return terminalRepository.findByMerchantId(merchantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TerminalResponse getTerminalByTerminalId(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new RuntimeException("Terminal not found: " + terminalId));
        return toResponse(terminal);
    }

    @Transactional
    public TerminalResponse deactivateTerminal(String terminalId) {
        Terminal terminal = terminalRepository.findByTerminalId(terminalId)
                .orElseThrow(() -> new RuntimeException("Terminal not found: " + terminalId));
        terminal.setActive(false);
        terminal = terminalRepository.save(terminal);
        log.info("Terminal deactivated: {}", terminalId);
        return toResponse(terminal);
    }

    private TerminalResponse toResponse(Terminal terminal) {
        return TerminalResponse.builder()
                .id(terminal.getId())
                .merchantId(terminal.getMerchantId())
                .terminalId(terminal.getTerminalId())
                .serialNumber(terminal.getSerialNumber())
                .model(terminal.getModel())
                .location(terminal.getLocation())
                .active(terminal.getActive())
                .createdAt(terminal.getCreatedAt())
                .build();
    }
}
