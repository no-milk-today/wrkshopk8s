package com.example.clients.order;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long customerId,
        String productDetails,
        Double amount,
        LocalDateTime orderDate
) {}
