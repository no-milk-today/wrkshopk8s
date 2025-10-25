package com.example.exchange.kafka;

import com.example.clients.exchange.ExchangeRequest;
import com.example.exchange.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateKafkaConsumer {

    private final ExchangeService exchangeService;

    @KafkaListener(topics = "exchange-rates", groupId = "exchange-service-group")
    public void listenExchangeRates(ConsumerRecord<String, ExchangeRequest> record) {
        log.info("Received message: key = {}, value = {}", record.key(), record.value());
        exchangeService.updateRates(record.value());
    }
}
