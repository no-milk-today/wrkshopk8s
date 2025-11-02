package com.example.exchange.kafka;

import com.example.clients.exchange.ExchangeRequest;
import com.example.exchange.service.ExchangeService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ExchangeRateKafkaConsumer {

    private final ExchangeService exchangeService;
    private final AtomicLong lastUpdateTimestamp = new AtomicLong(0);

    public ExchangeRateKafkaConsumer(ExchangeService exchangeService, MeterRegistry meterRegistry) {
        this.exchangeService = exchangeService;

        // Register Gauge metric for last update timestamp
        Gauge.builder("exchange_rates_last_update_timestamp_seconds", lastUpdateTimestamp, AtomicLong::get)
             .description("Timestamp (in seconds since epoch) of the last successful exchange rate update")
             .register(meterRegistry);

        log.info("Registered exchange_rates_last_update_timestamp_seconds gauge metric");
    }

    @KafkaListener(topics = "exchange-rates", groupId = "exchange-service-group")
    public void listenExchangeRates(ConsumerRecord<String, ExchangeRequest> record) {
        log.info("Received message: key = {}, value = {}", record.key(), record.value());
        exchangeService.updateRates(record.value());

        // Update timestamp after successful update
        long currentTimestamp = System.currentTimeMillis() / 1000; // Convert to seconds
        lastUpdateTimestamp.set(currentTimestamp);
        log.debug("Updated exchange rates timestamp to: {}", currentTimestamp);
    }
}
