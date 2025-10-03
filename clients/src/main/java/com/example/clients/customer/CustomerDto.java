package com.example.clients.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public record CustomerDto(
        Integer id,
        String login,
        String name,
        String email,
        LocalDate birthdate,
        List<AccountDto> accounts
) {

    /**
     * Поиск аккаунта по валюте
     */
    public AccountDto findAccountByCurrency(String currency) {
        return accounts.stream()
                .filter(account -> currency.equals(account.currencyCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Проверка наличия аккаунта с определенной валютой
     */
    public boolean hasAccountWithCurrency(String currency) {
        return findAccountByCurrency(currency) != null;
    }

    /**
     * Получение баланса аккаунта по валюте
     */
    public BigDecimal getBalanceForCurrency(String currency) {
        AccountDto account = findAccountByCurrency(currency);
        return account != null ? account.balance() : BigDecimal.ZERO;
    }
}