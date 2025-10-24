package com.example.exchangegenerator.service;

import com.example.clients.exchange.ExchangeRateDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeGeneratorService {

    private final KafkaTemplate<String, List<ExchangeRateDto>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private List<List<ExchangeRateDto>> exchangeRateSets;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    private static final String EXCHANGE_TOPIC = "exchange-rates";

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

        kafkaTemplate.send(EXCHANGE_TOPIC, "exchange-rates-update", rates)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send exchange rates to Kafka", ex);
                    } else {
                        log.info("Successfully sent exchange rates to Kafka topic: {}", EXCHANGE_TOPIC);
                    }
                });
    }

}
