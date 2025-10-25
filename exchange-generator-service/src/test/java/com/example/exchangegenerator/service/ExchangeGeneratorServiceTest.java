package com.example.exchangegenerator.service;

import com.example.clients.exchange.ExchangeRateDto;
import com.example.clients.exchange.ExchangeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   // Автоматически создаёт/закрывает моки
class ExchangeGeneratorServiceTest {

    @Mock
    private KafkaTemplate<String, ExchangeRequest> kafkaTemplate;
    @Mock
    private CompletableFuture<SendResult<String, ExchangeRequest>> completableFuture;

    private ObjectMapper objectMapper;
    private ExchangeGeneratorService service;

    @BeforeEach
    void init() {
        objectMapper = new ObjectMapper();
        service = new ExchangeGeneratorService(kafkaTemplate, objectMapper);
    }

    @Test
    void loadExchangeRates_loadsSetsCorrectly() throws Exception {
        var source = new File(getClass()
                .getClassLoader()
                .getResource("exchange-rates.json")
                .getFile());
        assertTrue(source.exists());

        service.loadExchangeRates();

        var field = service.getClass().getDeclaredField("exchangeRateSets");
        field.setAccessible(true);
        List<?> sets = (List<?>) field.get(service);

        assertThat(sets).isNotEmpty();
        assertThat(((List<?>)sets.getFirst()).getFirst()).isInstanceOf(ExchangeRateDto.class);
    }

    @Test
    void generateAndUpdateRates_sendsRoundRobinRates() {
        // init data
        service.loadExchangeRates();
        when(kafkaTemplate.send(any(), any(), any(ExchangeRequest.class))).thenReturn(completableFuture);

        service.generateAndUpdateRates(); // index 0
        service.generateAndUpdateRates(); // index 1

        verify(kafkaTemplate, times(2)).send(any(), any(), any(ExchangeRequest.class));
    }

    @AfterEach
    void verifyNoMore() {
        verifyNoMoreInteractions(kafkaTemplate);
    }
}