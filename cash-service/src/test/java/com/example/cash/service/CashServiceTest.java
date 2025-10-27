package com.example.cash.service;

import com.example.clients.cash.CashAction;
import com.example.clients.cash.CashOperationRequest;
import com.example.clients.customer.AccountDto;
import com.example.clients.customer.Currency;
import com.example.clients.customer.CustomerClient;
import com.example.clients.customer.CustomerDto;
import com.example.clients.notification.NotificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private CustomerClient customerClient;
    @Mock
    private KafkaTemplate<String, NotificationRequest> kafkaTemplate;
    @InjectMocks
    private CashService cashService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cashService, "cashNotificationTopic", "cash-notification");
    }

    private static CustomerDto buildCustomer(String login, BigDecimal rubBalance) {
        var rubAccount = new AccountDto(
                Currency.RUB,
                Currency.RUB.getCode(),
                Currency.RUB.getTitle(),
                rubBalance,
                true
        );
        return new CustomerDto(
                1,
                login,
                "Имя " + login,
                login + "@mail.io",
                LocalDate.of(1990, 1, 1),
                List.of(rubAccount)
        );
    }


    @Nested
    @DisplayName("DEPOSIT")
    class Deposit {

        @Test
        @DisplayName("Balance increase, notification sent")
        void deposit_ok() {
            var req = new CashOperationRequest(
                    "user5", "RUB",
                    BigDecimal.valueOf(100), CashAction.DEPOSIT);

            when(customerClient.getCustomer("user5"))
                    .thenReturn(buildCustomer("user5", BigDecimal.valueOf(200)));

            var response = cashService.processCashOperation(req);

            assertTrue(response.isSuccess());
            verify(customerClient).updateAccountBalance("user5", "RUB",
                    BigDecimal.valueOf(300));
            verify(kafkaTemplate).send(eq("cash-notification"), any(NotificationRequest.class));
        }
    }

    @Nested
    @DisplayName("WITHDRAW")
    class Withdraw {

        @Test
        @DisplayName("Not enough money, balance not changed, no notification")
        void withdraw_insufficient() {
            var req = new CashOperationRequest(
                    "user5", "RUB",
                    BigDecimal.valueOf(500), CashAction.WITHDRAW);

            when(customerClient.getCustomer("user5"))
                    .thenReturn(buildCustomer("user5", BigDecimal.valueOf(300)));

            var rs = cashService.processCashOperation(req);

            assertFalse(rs.isSuccess());
            verify(customerClient, never()).updateAccountBalance(any(), any(), any());
            verify(kafkaTemplate, never()).send(eq("cash-notification"), any(NotificationRequest.class));
        }
    }
}
