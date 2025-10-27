package com.example.exchangegenerator.service;

import com.example.clients.exchange.ExchangeRateDto;
import com.example.clients.exchange.ExchangeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@EmbeddedKafka(topics = {"exchange-rates"}, partitions = 1)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.profiles.active=default"
})
@ActiveProfiles("default")
@DirtiesContext
public class ExchangeGeneratorServiceIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ExchangeGeneratorService exchangeGeneratorService;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, ExchangeRequest> consumerForTest;

    @BeforeEach
    void setUp() {
        assertNotNull(exchangeGeneratorService, "ExchangeGeneratorService should be autowired");

        var valueDeserializer = new JsonDeserializer<>(ExchangeRequest.class, objectMapper);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeHeaders(false);

        var consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerForTest = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), valueDeserializer
        ).createConsumer();

        consumerForTest.subscribe(List.of("exchange-rates")); // ваш топик
    }

    @AfterEach
    void tearDown() {
        consumerForTest.close();
    }

    @Test
    public void testExchangeGeneratorServiceGeneratesRates() {
        // when
        exchangeGeneratorService.generateAndUpdateRates();

        // then
        var records = KafkaTestUtils.getRecords(consumerForTest, Duration.ofMillis(10000));
        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        var sentRequest = StreamSupport.stream(records.spliterator(), false)
                .filter(record -> "exchange-rates-update".equals(record.key()))
                .map(record -> record.value())
                .findFirst()
                .orElse(null);

        assertNotNull(sentRequest);
        assertThat(sentRequest.getRates()).isNotEmpty();

        for (ExchangeRateDto rate : sentRequest.getRates()) {
            assertThat(rate.getTitle()).isNotBlank();
            assertThat(rate.getName()).isNotBlank();
            assertThat(rate.getValue()).isGreaterThan(0);
        }

        var currencyCodes = sentRequest.getRates().stream()
                .map(ExchangeRateDto::getName)
                .toList();
        assertThat(currencyCodes).contains("USD", "CNY");
    }
}
