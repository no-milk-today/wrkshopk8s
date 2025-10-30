package com.example.fraud;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class FraudCheckService {

    private final FraudCheckHistoryRepository fraudCheckHistoryRepository;
    private final MeterRegistry meterRegistry;

    public boolean isFraudulentCustomer(Integer customerId) {
        // feel free to use 3d party system
        // that checks whether a customer is fraudster or not
        boolean isFraud = false; //todo: implement actual fraud check logic

        fraudCheckHistoryRepository.save(
                FraudCheckHistory.builder()
                        .customerId(customerId)
                        .isFraudster(isFraud)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // metric for fraud checks
        String result = isFraud ? "fraud" : "clean";
        meterRegistry.counter("fraud_checks_total", "result", result).increment();

        return isFraud;
    }
}
