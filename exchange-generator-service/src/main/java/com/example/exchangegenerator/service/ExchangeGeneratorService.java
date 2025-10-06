package com.example.exchangegenerator.service;

import com.example.clients.exchange.ExchangeClient;
import com.example.clients.exchange.ExchangeRateDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeGeneratorService {

    private final ExchangeClient exchangeClient;
    private final ObjectMapper objectMapper;

    private List<List<ExchangeRateDto>> exchangeRateSets;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @PostConstruct
    public void loadExchangeRates() {
        try {
            var resource = new ClassPathResource("exchange-rates.json");
            exchangeRateSets = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<List<ExchangeRateDto>>>() {}
            );
            log.info("Loaded {} exchange rate sets from file", exchangeRateSets.size());
        } catch (IOException e) {
            log.error("Failed to load exchange rates from file", e);
            throw new RuntimeException("Cannot load exchange rates", e);
        }
    }

    @Scheduled(fixedRate = 5000) // every 5 seconds
    @CircuitBreaker(name = "exchange-client", fallbackMethod = "updateRatesFallback")
    @Retry(name = "exchange-client")
    public void generateAndUpdateRates() {
        if (exchangeRateSets == null || exchangeRateSets.isEmpty()) {
            log.warn("No exchange rate sets available");
            return;
        }

        // Round Robin: получаем следующий набор курсов
        int index = currentIndex.getAndUpdate(i -> (i + 1) % exchangeRateSets.size());
        List<ExchangeRateDto> rates = exchangeRateSets.get(index);

        log.info("Updating exchange rates (set {} of {}): {}",
                index + 1, exchangeRateSets.size(), rates);

        exchangeClient.updateRates(rates);
        log.info("Successfully updated exchange rates");
    }

    public void updateRatesFallback(Exception ex) {
        log.error("Exchange service unavailable: {}. Sending default exchange rates.", ex.getMessage());

        List<ExchangeRateDto> defaultRates = List.of(
                new ExchangeRateDto("Рубль", "RUB", 1.0),
                new ExchangeRateDto("Доллар", "USD", 95.00),
                new ExchangeRateDto("Юань", "CNY", 13.50)
        );
        try {
            // Повторная попытка выставить стандартные значения
            exchangeClient.updateRates(defaultRates);
            log.info("Default exchange rates have been sent to exchange service in fallback.");
        } catch (Exception fatal) {
            log.error("Failed to send default exchange rates in fallback scenario: {}", fatal.getMessage());
        }
    }
}
