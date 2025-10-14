package com.example.transfer.service;

import com.example.clients.customer.AccountDto;
import com.example.clients.customer.Currency;
import com.example.clients.customer.CustomerClient;
import com.example.clients.customer.CustomerDto;
import com.example.clients.exchange.ExchangeClient;
import com.example.clients.exchange.ExchangeRateDto;
import com.example.clients.fraud.FraudCheckResponse;
import com.example.clients.fraud.FraudClient;
import com.example.clients.notification.NotificationClient;
import com.example.clients.notification.NotificationRequest;
import com.example.clients.transfer.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CustomerClient customerClient;
    @Mock
    private FraudClient fraudClient;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private ExchangeClient exchangeClient;
    @InjectMocks
    private TransferService underTest;

    private static CustomerDto buildCustomer(int id, String login, BigDecimal balance) {
        var accDTO = new AccountDto(
                Currency.RUB,
                Currency.RUB.getCode(),
                Currency.RUB.getTitle(),
                balance,
                true
        );
        return new CustomerDto(
                id,
                login,
                "Name " + login,
                login + "@example.com",
                LocalDate.of(1995, 1, 1),
                List.of(accDTO)
        );
    }

    @Test
    void transfer_success_external() {
        var alice = buildCustomer(1, "alice", BigDecimal.valueOf(1000));
        var bob   = buildCustomer(2, "bob",   BigDecimal.valueOf(200));
        when(customerClient.getCustomer("alice")).thenReturn(alice);
        when(customerClient.getCustomer("bob")).thenReturn(bob);
        when(fraudClient.isFraudster(1))
                .thenReturn(new FraudCheckResponse(false));

        var req = new TransferRequest(
                "RUB",
                "RUB",
                BigDecimal.valueOf(150),
                "bob"
        );

        var resp = underTest.transfer("alice", req);

        assertTrue(resp.isSuccess());
        // balances
        verify(customerClient).updateAccountBalance("alice", "RUB", BigDecimal.valueOf(850));
        verify(customerClient).updateAccountBalance("bob",   "RUB", BigDecimal.valueOf(350));
        // notifications
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient, times(2)).sendNotification(captor.capture());
        List<NotificationRequest> sent = captor.getAllValues();
        assertThat(sent)
                .extracting(NotificationRequest::toCustomerId, NotificationRequest::toCustomerName)
                .containsExactlyInAnyOrder(
                        tuple(1, "Name alice"),
                        tuple(2, "Name bob")
                );
    }

    @Test
    void transfer_fraud_detected() {
        var alice = buildCustomer(1, "alice", BigDecimal.valueOf(500));
        when(customerClient.getCustomer("alice")).thenReturn(alice);
        when(fraudClient.isFraudster(1))
                .thenReturn(new FraudCheckResponse(true));

        var req = new TransferRequest(
                "RUB", "RUB", BigDecimal.valueOf(100), "bob"
        );

        var resp = underTest.transfer("alice", req);

        assertFalse(resp.isSuccess());
        assertThat(resp.getTransferErrors()).containsExactly("Fraud detected");
        verify(customerClient, never()).updateAccountBalance(anyString(), anyString(), any());
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void currency_conversion() {
        when(fraudClient.isFraudster(1)).thenReturn(new FraudCheckResponse(false));

        var usdAccount = new AccountDto(
                Currency.USD,
                Currency.USD.getCode(),
                Currency.USD.getTitle(),
                BigDecimal.valueOf(100),
                true
        );
        var user1 = new CustomerDto(
                1,
                "user1",
                "Name user1",
                "user1@example.com",
                LocalDate.of(1995, 1, 1),
                List.of(usdAccount)
        );

        var rubAccount = new AccountDto(
                Currency.RUB,
                Currency.RUB.getCode(),
                Currency.RUB.getTitle(),
                BigDecimal.ZERO,
                true
        );
        var user2 = new CustomerDto(
                2,
                "user2",
                "Name user2",
                "user2@example.com",
                LocalDate.of(1995, 1, 1),
                List.of(rubAccount)
        );

        when(customerClient.getCustomer("user1")).thenReturn(user1);
        when(customerClient.getCustomer("user2")).thenReturn(user2);

        var rates = List.of(
                new ExchangeRateDto("Доллар", "USD", 95.0),
                new ExchangeRateDto("Рубль",  "RUB", 1.0)
        );
        when(exchangeClient.getRates()).thenReturn(rates);

        var transferRequest = new TransferRequest("USD", "RUB",
                BigDecimal.valueOf(10), "user2");

        var transferResponse = underTest.transfer("user1", transferRequest);

        assertTrue(transferResponse.isSuccess());
        verify(exchangeClient).getRates();

        verify(customerClient).updateAccountBalance("user1", "USD", BigDecimal.valueOf(90));
        verify(customerClient).updateAccountBalance("user2", "RUB", BigDecimal.valueOf(950));
    }

}