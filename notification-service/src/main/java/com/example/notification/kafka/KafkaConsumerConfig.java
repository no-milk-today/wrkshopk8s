package com.example.notification.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonDelegatingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    CommonErrorHandler commonErrorHandler(DeadLetterQueueRecordRecoverer deadLetterQueueRecordRecoverer) {
        // By default, this handler will perform 9 retries with no delay.
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(deadLetterQueueRecordRecoverer);

        // This handler will send the message to the DLQ immediately (0 retries).
        DefaultErrorHandler deadLetterQueueErrorHandler = new DefaultErrorHandler(deadLetterQueueRecordRecoverer, new FixedBackOff(0, 0));

        // Delegate to deadLetterQueueErrorHandler for IllegalArgumentException
        CommonDelegatingErrorHandler errorHandler = new CommonDelegatingErrorHandler(defaultErrorHandler);
        errorHandler.setErrorHandlers(Map.of(IllegalArgumentException.class, deadLetterQueueErrorHandler));

        return errorHandler;
    }
}
