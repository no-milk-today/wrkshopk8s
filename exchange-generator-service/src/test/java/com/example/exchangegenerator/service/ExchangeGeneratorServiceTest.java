package com.example.exchangegenerator.service;

import com.example.clients.exchange.ExchangeClient;
import com.example.clients.exchange.ExchangeRateDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   // Автоматически создаёт/закрывает моки
class ExchangeGeneratorServiceTest {

    @Mock
    private ExchangeClient exchangeClient;

    private ObjectMapper objectMapper;
    private ExchangeGeneratorService service;

    @BeforeEach
    void init() {
        objectMapper = new ObjectMapper();
        service = new ExchangeGeneratorService(exchangeClient, objectMapper);
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

        service.generateAndUpdateRates(); // index 0
        service.generateAndUpdateRates(); // index 1

        verify(exchangeClient, times(2)).updateRates(any());
    }

    @AfterEach
    void verifyNoMore() {
        verifyNoMoreInteractions(exchangeClient);
    }
}