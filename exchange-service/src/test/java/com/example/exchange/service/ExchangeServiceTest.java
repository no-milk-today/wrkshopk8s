package com.example.exchange.service;

import com.example.clients.customer.Currency;
import com.example.clients.exchange.ExchangeRateDto;
import com.example.exchange.mapper.ExchangeRateFromDTOConverter;
import com.example.exchange.mapper.ExchangeRateToDTOConverter;
import com.example.exchange.model.ExchangeRate;
import com.example.exchange.repository.ExchangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExchangeServiceTest {

    @Mock
    private ExchangeRepository repo;

    @InjectMocks
    private ExchangeService underTest;

    private final ExchangeRateToDTOConverter toDto = new ExchangeRateToDTOConverter();
    private final ExchangeRateFromDTOConverter fromDto = new ExchangeRateFromDTOConverter();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        underTest = new ExchangeService(repo, toDto, fromDto);
    }

    @Test
    void getAllRates_includesBaseRub() {
        var usd = ExchangeRate.builder()
                .currency(Currency.USD)
                .value(93.5).build();

        when(repo.findAll()).thenReturn(List.of(usd));

        var rates = underTest.getAllRates();

        assertThat(rates).hasSize(2)
                .extracting(ExchangeRateDto::getName)
                .containsExactly("RUB", "USD");
    }

    @Test
    void updateRates_savesOnlyNonRub() {
        var r1 = new ExchangeRateDto("Рубль", "RUB", 1.0);
        var r2 = new ExchangeRateDto("Доллар", "USD", 94.0);
        var r3 = new ExchangeRateDto("Юань", "CNY", 13.2);

        underTest.updateRates(List.of(r1, r2, r3));

        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(repo).deleteAll();
        verify(repo).saveAll(captor.capture());

        var saved = captor.getValue();
        assertThat(saved).hasSize(2)
                .extracting(ExchangeRate::getCurrency)
                .containsExactly(Currency.USD, Currency.CNY);
    }
}