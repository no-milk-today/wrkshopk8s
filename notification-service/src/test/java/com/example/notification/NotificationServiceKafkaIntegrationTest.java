package com.example.notification;

import com.example.clients.notification.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Disabled
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext
@TestPropertySource(properties = {
        "spring.profiles.active=default"
})
class NotificationServiceKafkaIntegrationTest {

    private static final String NOTIFICATION_TOPIC = "notification-topic";

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
