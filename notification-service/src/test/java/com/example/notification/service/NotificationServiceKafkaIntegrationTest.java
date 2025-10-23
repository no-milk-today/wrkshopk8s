package com.example.notification.service;

import com.example.clients.notification.NotificationRequest;
import com.example.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@EmbeddedKafka(partitions = 1, topics = {NotificationServiceKafkaIntegrationTest.NOTIFICATION_TOPIC}, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext
@TestPropertySource(properties = {
        "spring.profiles.active=default"
})
class NotificationServiceKafkaIntegrationTest {

    static final String NOTIFICATION_TOPIC = "customer-notification";

    @Autowired
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void notificationService_shouldConsumeMessageAndSaveNotification() {
        // Given
        var request = new NotificationRequest(
                1, "test@example.com", "Test message from Kafka"
        );

        // When
        kafkaTemplate.send(NOTIFICATION_TOPIC, request);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(notificationRepository.findAll()).hasSize(1);
            var savedNotification = notificationRepository.findAll().getFirst();
            assertThat(savedNotification.getToCustomerId()).isEqualTo(request.toCustomerId());
            assertThat(savedNotification.getToCustomerEmail()).isEqualTo(request.toCustomerName());
            assertThat(savedNotification.getMessage()).isEqualTo(request.message());
        });
    }
}
