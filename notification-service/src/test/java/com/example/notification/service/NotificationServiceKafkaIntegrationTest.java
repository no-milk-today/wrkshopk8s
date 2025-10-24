package com.example.notification.service;

import com.example.clients.notification.NotificationRequest;
import com.example.notification.AbstractIntegrationTest;
import com.example.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EmbeddedKafka(partitions = 1, topics = { "customer-notification", "cash-notification", "transfer-notification" }, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext
@TestPropertySource(properties = {
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
class NotificationServiceKafkaIntegrationTest extends AbstractIntegrationTest {

    private static final String CUSTOMER_NOTIFICATION_TOPIC = "customer-notification";
    private static final String CASH_NOTIFICATION_TOPIC = "cash-notification";
    private static final String TRANSFER_NOTIFICATION_TOPIC = "transfer-notification";

    @Autowired
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldConsumeCustomerNotification() {
        // Given
        var request = new NotificationRequest(
                1, "test@example.com", "Test message from Customer Service"
        );

        // When
        kafkaTemplate.send(CUSTOMER_NOTIFICATION_TOPIC, request);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(notificationRepository.findAll()).hasSize(1);
            var savedNotification = notificationRepository.findAll().getFirst();
            assertThat(savedNotification.getToCustomerId()).isEqualTo(request.toCustomerId());
            assertThat(savedNotification.getToCustomerEmail()).isEqualTo(request.toCustomerName());
            assertThat(savedNotification.getMessage()).isEqualTo(request.message());
        });
    }

    @Test
    void shouldConsumeCashNotification() {
        // Given
        var request = new NotificationRequest(
                2, "cash@example.com", "Test message from Cash Service"
        );

        // When
        kafkaTemplate.send(CASH_NOTIFICATION_TOPIC, request);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(notificationRepository.findAll()).hasSize(1);
            var savedNotification = notificationRepository.findAll().getFirst();
            assertThat(savedNotification.getToCustomerId()).isEqualTo(request.toCustomerId());
            assertThat(savedNotification.getToCustomerEmail()).isEqualTo(request.toCustomerName());
            assertThat(savedNotification.getMessage()).isEqualTo(request.message());
        });
    }

    @Test
    void shouldConsumeTransferNotification() {
        // Given
        var request = new NotificationRequest(
                3, "transfer@example.com", "Test message from Transfer Service"
        );

        // When
        kafkaTemplate.send(TRANSFER_NOTIFICATION_TOPIC, request);

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
