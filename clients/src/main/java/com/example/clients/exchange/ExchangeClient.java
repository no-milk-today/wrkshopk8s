package com.example.clients.exchange;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
        name = "exchange",
        url = "${clients.exchange.url}"
)
public interface ExchangeClient {

    @GetMapping("/api/rates")
    List<ExchangeRateDto> getRates();

}
