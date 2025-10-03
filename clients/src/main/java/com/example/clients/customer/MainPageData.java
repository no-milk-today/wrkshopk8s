package com.example.clients.customer;

import java.time.LocalDate;
import java.util.List;

public record MainPageData(
        String login,
        String name,
        String email,
        LocalDate birthdate,
        List<AccountDto> accounts,
        List<CurrencyDto> currency,
        List<UserShortDto> users,
        List<String> passwordErrors,
        List<String> userAccountsErrors,
        List<String> cashErrors,
        List<String> transferErrors,
        List<String> transferOtherErrors
) {}
