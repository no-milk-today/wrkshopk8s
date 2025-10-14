package com.example.cash.controller;

import com.example.cash.service.CashService;
import com.example.clients.cash.CashOperationRequest;
import com.example.clients.cash.CashOperationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/v1/cash")
@RequiredArgsConstructor
@Slf4j
public class CashController {

    private final CashService cashService;

    @PostMapping("/operation")
    public ResponseEntity<CashOperationResponse> processCashOperation(
            @RequestBody CashOperationRequest request) {

        log.info("Processing cash operation: {} {} {} for user {}",
                request.action(), request.value(), request.currency(), request.login());

        try {
            var response = cashService.processCashOperation(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing cash operation for user {}: {}", request.login(), e.getMessage(), e);
            return ResponseEntity.ok(new CashOperationResponse(false,
                    List.of("Error processing operation: " + e.getMessage())));
        }
    }
}
