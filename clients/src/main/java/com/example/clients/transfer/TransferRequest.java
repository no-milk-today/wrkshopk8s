package com.example.clients.transfer;

import java.math.BigDecimal;

public record TransferRequest(
        String fromCurrency,
        String toCurrency,
        BigDecimal value,
        String toLogin
) { }
