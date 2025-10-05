package com.example.exchange.controller;

import com.example.clients.exchange.ExchangeRateDto;
import com.example.exchange.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class ExchangeController {
    private final ExchangeService exchangeService;

    @GetMapping
    public List<ExchangeRateDto> getRates() {
        return exchangeService.getAllRates();
    }

    // it is used by exchange-generator service to update exchange rates via REST API
    @PutMapping("/update")
    public void updateRates(@RequestBody List<ExchangeRateDto> rates) {
        exchangeService.updateRates(rates);
    }
}

