package com.example.clients.exchange;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDto {
    private String title;    // название валюты
    private String name;     // код валюты, e.g. "USD"
    private double value;    // курс по отношению к RUB
}
