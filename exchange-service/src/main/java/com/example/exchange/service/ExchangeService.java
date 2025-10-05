package com.example.exchange.service;

import com.example.clients.customer.Currency;
import com.example.clients.exchange.ExchangeRateDto;
import com.example.exchange.mapper.ExchangeRateFromDTOConverter;
import com.example.exchange.mapper.ExchangeRateToDTOConverter;
import com.example.exchange.model.ExchangeRate;
import com.example.exchange.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final ExchangeRepository repo;
    private final ExchangeRateToDTOConverter toDto;
    private final ExchangeRateFromDTOConverter fromDto;

    /** GET /api/rates */
    public List<ExchangeRateDto> getAllRates() {
        log.debug("Fetching all exchange rates");
        List<ExchangeRateDto> out = new ArrayList<>();
        // базовый рубль
        out.add(new ExchangeRateDto(
                Currency.RUB.getTitle(),
                Currency.RUB.name(),
                1.0
        ));
        // остальные из БД
        repo.findAll().stream()
                .map(toDto)
                .forEach(out::add);
        return out;
    }

    /** POST /api/rates */
    @Transactional
    public void updateRates(List<ExchangeRateDto> rates) {
        log.info("Received request to update exchange rates. Deleting existing rates...");
        repo.deleteAll();
        List<ExchangeRate> ents = rates.stream()
                .filter(r -> !Currency.RUB.name().equals(r.getName()))
                .map(fromDto)
                .toList();
        repo.saveAll(ents);
        log.info("Successfully updated exchange rates");
    }
}

