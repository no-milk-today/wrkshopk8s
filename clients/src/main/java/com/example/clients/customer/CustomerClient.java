package com.example.clients.customer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(
        name = "customer",
        url = "${clients.customer.url}"
)
public interface CustomerClient {

    @GetMapping("/api/v1/customers/main")
    MainPageData getMainPage(@RequestParam("login") String login);

    @PostMapping("/api/v1/customers/user/{login}/editPassword")
    List<String> editPassword(
            @PathVariable("login") String login,
            @RequestBody EditPasswordRequest request
    );

    @PostMapping("/api/v1/customers/signup")
    CustomerRegistrationResponse registerCustomer(
            @RequestBody CustomerRegistrationRequest request
    );

    @PostMapping("/api/v1/customers/user/{login}/editUserAccounts")
    List<String> editUserAccounts(
            @PathVariable("login") String login,
            @RequestBody EditUserAccountsRequest request
    );

    @PutMapping("/api/v1/customers/{login}/accounts/{currency}/balance")
    void updateAccountBalance(
            @PathVariable("login") String login,
            @PathVariable("currency") String currency,
            @RequestBody BigDecimal newBalance
    );

    @GetMapping("/api/v1/customers/{login}")
    CustomerDto getCustomer(@PathVariable("login") String login);

}
