package com.example.customerservice;

import com.example.clients.customer.CustomerRegistrationRequest;
import com.example.clients.fraud.FraudCheckResponse;
import com.example.clients.fraud.FraudClient;
import com.example.clients.notification.NotificationRequest;
import com.example.customerservice.repository.CustomerRepository;
import com.example.customerservice.service.CustomerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class CustomerServiceKafkaIntegrationTest extends AbstractIntegrationTest {

    private static final String NOTIFICATION_TOPIC = "notification-topic";

    @Autowired
    private CustomerService customerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private CustomerRepository customerRepository;

    @MockitoBean
    private FraudClient fraudClient;

    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll(); // Clean up before each test
        // Add fraud client mocking
        when(fraudClient.isFraudster(anyInt())).thenReturn(new FraudCheckResponse(false));

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(NOTIFICATION_TOPIC);
        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(cf, containerProperties);
        consumerRecords = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, String>) consumerRecords::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @Test
    void registerCustomer_shouldSendNotificationToKafka() throws InterruptedException {
        // Given
        var request = new CustomerRegistrationRequest(
                "john.doe", "password123", "password123", "John Doe", "john.doe@example.com", LocalDate.of(1990, 1, 1)
        );

        // When
        customerService.registerCustomer(request);

        // Then
        var received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(NOTIFICATION_TOPIC, received.topic());
        assertThat(received.value()).contains("Hi John Doe, welcome to Bank-system...");
    }
}
