package com.example.transfer.controller;

import com.example.clients.transfer.TransferRequest;
import com.example.clients.transfer.TransferResponse;
import com.example.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/{login}")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    @PostMapping(path = "/transfer",
            consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<TransferResponse> transfer(
            @PathVariable("login") String login,
            @RequestBody TransferRequest request) {

        log.info("Processing transfer for user {}: {} → {}, amount {} to {}",
                login,
                request.fromCurrency(),
                request.toCurrency(),
                request.value(),
                request.toLogin());

        try {
            var response = transferService.transfer(login, request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error processing transfer for {}: {}", login, ex.getMessage(), ex);
            return ResponseEntity.ok(
                    TransferResponse.error("Error processing transfer: " + ex.getMessage())
            );
        }
    }
}
