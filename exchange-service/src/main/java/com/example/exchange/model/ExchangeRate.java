package com.example.exchange.model;

import com.example.clients.customer.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency currency;   // ENUM: RUB, USD, CNY

    @Column(nullable = false)
    private double value;        // курс к RUB
}
