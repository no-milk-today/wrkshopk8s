package com.example.clients.cash;

import java.math.BigDecimal;

/**
 * DTO для запроса операции с деньгами
 */
public record CashOperationRequest(
        String login,
        String currency,
        BigDecimal value,
        CashAction action
) {

    /**
     * Валидация запроса
     */
    public boolean isValid() {
        return login != null && !login.trim().isEmpty() &&
                currency != null && !currency.trim().isEmpty() &&
                value != null && value.compareTo(BigDecimal.ZERO) > 0 &&
                action != null;
    }
}
