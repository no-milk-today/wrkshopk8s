package com.example.clients.customer;

import java.time.LocalDate;
import java.util.List;

public record EditUserAccountsRequest(
        String name,
        LocalDate birthdate,
        List<String> accounts
) {}
