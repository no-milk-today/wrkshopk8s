package com.example.cash.service;

import com.example.clients.cash.CashAction;
import com.example.clients.cash.CashOperationRequest;
import com.example.clients.customer.AccountDto;
import com.example.clients.customer.Currency;
import com.example.clients.customer.CustomerClient;
import com.example.clients.customer.CustomerDto;
import com.example.clients.notification.NotificationRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext
@TestPropertySource(properties = {
        "spring.profiles.active=default"
})
class CashServiceKafkaIntegrationTest {

    private static final String NOTIFICATION_TOPIC = "cash-notification";

    @Autowired
    private CashService cashService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private CustomerClient customerClient;

    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;

    @BeforeEach
    void setUp() {
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
    void processCashOperation_shouldSendNotificationToKafka() throws InterruptedException {
        // Given
        var login = "testUser";
        var currency = Currency.RUB.getCode();
        var initialBalance = BigDecimal.valueOf(1000);
        var operationValue = BigDecimal.valueOf(100);

        var customer = new CustomerDto(1, login, "Test User", "test@example.com", null, List.of(
                new AccountDto(com.example.clients.customer.Currency.RUB, currency, "Рубль", initialBalance, true)
        ));

        when(customerClient.getCustomer(login)).thenReturn(customer);
        doNothing().when(customerClient).updateAccountBalance(eq(login), eq(currency), any(BigDecimal.class));

        var request = new CashOperationRequest(login, currency, operationValue, CashAction.DEPOSIT);

        // When
        cashService.processCashOperation(request);

        // Then
        var received = consumerRecords.poll(10, TimeUnit.SECONDS);
        assertNotNull(received);
        assertThat(received.topic()).isEqualTo(NOTIFICATION_TOPIC);
        assertThat(received.value()).contains("Выполнено пополнение счета на сумму 100 RUB. Новый баланс: 1100 RUB");
    }
}
