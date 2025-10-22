package com.example.transfer.service;

import com.example.clients.customer.AccountDto;
import com.example.clients.customer.Currency;
import com.example.clients.customer.CustomerClient;
import com.example.clients.customer.CustomerDto;
import com.example.clients.exchange.ExchangeClient;
import com.example.clients.exchange.ExchangeRateDto;
import com.example.clients.fraud.FraudClient;
import com.example.clients.fraud.FraudCheckResponse;
import com.example.clients.transfer.TransferRequest;
import org.apache.kafka.clients.admin.NewTopic;
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
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.kafka.test.utils.ContainerTestUtils.waitForAssignment;

@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@DirtiesContext
@TestPropertySource(properties = {
        "spring.profiles.active=default"
})
class TransferServiceKafkaIntegrationTest {

    private static final String NOTIFICATION_TOPIC = "notification-topic";

    @Autowired
    private TransferService transferService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private CustomerClient customerClient;

    @MockitoBean
    private FraudClient fraudClient;

    @MockitoBean
    private ExchangeClient exchangeClient;

    private BlockingQueue<ConsumerRecord<String, String>> consumerRecords;

    @BeforeEach
    void setUp() {
        // Clear any existing records
        if (consumerRecords != null) {
            consumerRecords.clear();
        }

        // Use unique consumer group and client IDs
        String uniqueId = UUID.randomUUID().toString();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-" + uniqueId,
                "true",
                embeddedKafkaBroker
        );
        consumerProps.put("client.id", "test-client-" + uniqueId);

        var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        var containerProperties = new ContainerProperties(NOTIFICATION_TOPIC);
        var container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        consumerRecords = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, String>) consumerRecords::add);
        container.start();

        waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @Test
    void transfer_shouldSendNotificationToKafkaForSenderAndReceiver() throws InterruptedException {
        // Given
        var senderLogin = "sender";
        var receiverLogin = "receiver";
        var fromCurrency = Currency.RUB.getCode();
        var toCurrency = Currency.USD.getCode();
        var transferValue = BigDecimal.valueOf(100);

        var sender = new CustomerDto(1, senderLogin, "Sender Name", "sender@example.com", null, List.of(
                new AccountDto(com.example.clients.customer.Currency.RUB, fromCurrency, "Рубль", BigDecimal.valueOf(1000), true)
        ));
        var receiver = new CustomerDto(2, receiverLogin, "Receiver Name", "receiver@example.com", null, List.of(
                new AccountDto(com.example.clients.customer.Currency.USD, toCurrency, "Доллар", BigDecimal.valueOf(50), true)
        ));

        when(customerClient.getCustomer(senderLogin)).thenReturn(sender);
        when(customerClient.getCustomer(receiverLogin)).thenReturn(receiver);
        when(fraudClient.isFraudster(any())).thenReturn(new FraudCheckResponse(false));
        when(exchangeClient.getRates()).thenReturn(List.of(
                new ExchangeRateDto("Рубль", "RUB", 1.0),
                new ExchangeRateDto("Доллар", "USD", 90.0)
        ));
        doNothing().when(customerClient).updateAccountBalance(any(), any(), any());

        var request = new TransferRequest(fromCurrency, toCurrency, transferValue, receiverLogin);

        // When
        transferService.transfer(senderLogin, request);

        // Then
        var senderNotification = consumerRecords.poll(10, TimeUnit.SECONDS);
        var receiverNotification = consumerRecords.poll(10, TimeUnit.SECONDS);

        assertThat(senderNotification).isNotNull();
        assertThat(senderNotification.topic()).isEqualTo(NOTIFICATION_TOPIC);
        assertThat(senderNotification.value()).contains("Перевод 100.00 RUB -> 1.00 USD от sender к receiver");

        assertThat(receiverNotification).isNotNull();
        assertThat(receiverNotification.topic()).isEqualTo(NOTIFICATION_TOPIC);
        assertThat(receiverNotification.value()).contains("Перевод 100.00 RUB -> 1.00 USD от sender к receiver");
    }

    @Test
    void transfer_shouldSendNotificationToKafkaForSenderOnlyIfSameUser() throws InterruptedException {
        // Given
        var senderLogin = "sender";
        var fromCurrency = "RUB";
        var toCurrency = "USD";
        var transferValue = BigDecimal.valueOf(100);

        var sender = new CustomerDto(1, senderLogin, "Sender Name", "sender@example.com", null, List.of(
                new AccountDto(Currency.RUB, fromCurrency, "Рубль", BigDecimal.valueOf(1000), true),
                new AccountDto(Currency.USD, toCurrency, "Доллар", BigDecimal.valueOf(50), true)
        ));

        // Important: Use senderLogin for both sender and receiver lookups
        when(customerClient.getCustomer(eq(senderLogin))).thenReturn(sender);
        when(fraudClient.isFraudster(any())).thenReturn(new FraudCheckResponse(false));
        when(exchangeClient.getRates()).thenReturn(List.of(
                new ExchangeRateDto("Рубль", "RUB", 1.0),
                new ExchangeRateDto("Доллар", "USD", 90.0)
        ));
        doNothing().when(customerClient).updateAccountBalance(any(), any(), any());

        var request = new TransferRequest(fromCurrency, toCurrency, transferValue, senderLogin);

        // When
        transferService.transfer(senderLogin, request);

        // Then
        var notification = consumerRecords.poll(10, TimeUnit.SECONDS);
        var noOtherNotification = consumerRecords.poll(1, TimeUnit.SECONDS);

        assertNotNull(notification);
        assertThat(notification.topic()).isEqualTo(NOTIFICATION_TOPIC);

        // Verify the JSON structure contains expected message
        String notificationJson = notification.value();
        assertThat(notificationJson)
                .contains("\"toCustomerId\":1")
                .contains("\"toCustomerName\":\"Sender Name\"")
                .contains(String.format("\"message\":\"Перевод 100.00 RUB -> 1.00 USD от %s к %s\"", senderLogin, senderLogin));

        assertNull(noOtherNotification, "Should not receive second notification for same-user transfer");
    }

    @Test
    void transfer_shouldFailOnInsufficientFunds() throws InterruptedException {
        // Given
        var senderLogin = "sender";
        var receiverLogin = "receiver";
        var fromCurrency = "RUB";
        var toCurrency = "RUB";
        var transferValue = BigDecimal.valueOf(1000);

        var sender = new CustomerDto(1, senderLogin, "Sender Name", "sender@example.com", null, List.of(
                new AccountDto(Currency.RUB, fromCurrency, "Рубль", BigDecimal.valueOf(500), true)
        ));

        when(customerClient.getCustomer(eq(senderLogin))).thenReturn(sender);
        // Mock receiver as well to prevent NullPointerException when accessing its accounts
        var receiver = new CustomerDto(2, receiverLogin, "Receiver Name", "receiver@example.com", null, List.of(
                new AccountDto(Currency.RUB, toCurrency, "Рубль", BigDecimal.valueOf(0), true)
        ));
        when(customerClient.getCustomer(eq(receiverLogin))).thenReturn(receiver);
        when(fraudClient.isFraudster(any())).thenReturn(new FraudCheckResponse(false));

        var request = new TransferRequest(fromCurrency, toCurrency, transferValue, receiverLogin);

        // When
        var response = transferService.transfer(senderLogin, request);

        // Then
        assertFalse(response.isSuccess());
        assertThat(response.getTransferErrors()).containsExactly("Insufficient funds");
        verify(customerClient, never()).updateAccountBalance(any(), any(), any());

        // Ensure no notifications are sent
        var notification = consumerRecords.poll(1, TimeUnit.SECONDS);
        assertNull(notification, "No notification should be sent for insufficient funds");
    }

    @Test
    void transfer_shouldFailOnCurrencyConversionFailure() throws InterruptedException {
        // Given
        var senderLogin = "sender";
        var receiverLogin = "receiver";
        var fromCurrency = "RUB";
        var toCurrency = "XYZ";
        var transferValue = BigDecimal.valueOf(100);

        var sender = new CustomerDto(1, senderLogin, "Sender Name", "sender@example.com", null, List.of(
                new AccountDto(Currency.RUB, fromCurrency, "Рубль", BigDecimal.valueOf(1000), true)
        ));

        when(customerClient.getCustomer(eq(senderLogin))).thenReturn(sender);
        // Mock receiver as well to prevent NullPointerException when accessing its accounts
        var receiver = new CustomerDto(2, receiverLogin, "Receiver Name", "receiver@example.com", null, List.of(
                new AccountDto(Currency.RUB, fromCurrency, "Рубль", BigDecimal.valueOf(0), true)
        ));
        when(customerClient.getCustomer(eq(receiverLogin))).thenReturn(receiver);
        when(fraudClient.isFraudster(any())).thenReturn(new FraudCheckResponse(false));
        when(exchangeClient.getRates()).thenReturn(List.of(
                new ExchangeRateDto("Рубль", "RUB", 1.0)
        ));

        var request = new TransferRequest(fromCurrency, toCurrency, transferValue, receiverLogin);

        // When
        var response = transferService.transfer(senderLogin, request);

        // Then
        assertFalse(response.isSuccess());
        assertThat(response.getTransferErrors())
                .containsExactly("Currency conversion failed: Exchange rate not found for XYZ");
        verify(customerClient, never()).updateAccountBalance(any(), any(), any());

        // Ensure no notifications are sent
        var notification = consumerRecords.poll(1, TimeUnit.SECONDS);
        assertNull(notification, "No notification should be sent for currency conversion failure");
    }

}
