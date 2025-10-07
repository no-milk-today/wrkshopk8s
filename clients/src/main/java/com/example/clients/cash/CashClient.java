package com.example.clients.cash;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "cash",
        url = "${clients.cash.url}"
)
public interface CashClient {

    @PostMapping("/api/v1/cash/operation")
    CashOperationResponse processCashOperation(@RequestBody CashOperationRequest request);
}
