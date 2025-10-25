package com.example.exchange.controller;

import com.example.clients.exchange.ExchangeRateDto;
import com.example.exchange.service.ExchangeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeController.class)
@TestPropertySource(properties = {
        "spring.profiles.active=default"
})
class ExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExchangeService exchangeService;

    @Test
    void getRates() throws Exception {
        var list = List.of(
                new ExchangeRateDto("Рубль","RUB",1.0),
                new ExchangeRateDto("Доллар","USD",93.5)
        );
        when(exchangeService.getAllRates()).thenReturn(list);

        mockMvc.perform(get("/api/rates").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("USD"));
    }
}