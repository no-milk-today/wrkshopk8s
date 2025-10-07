package com.example.frontui;

import com.example.clients.cash.CashAction;
import com.example.clients.cash.CashClient;
import com.example.clients.cash.CashOperationRequest;
import com.example.clients.customer.*;
import com.example.clients.exchange.ExchangeClient;
import com.example.clients.exchange.ExchangeRateDto;
import com.example.clients.transfer.TransferClient;
import com.example.clients.transfer.TransferRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FrontUiService {

    private final CustomerClient customerClient;
    private final ExchangeClient exchangeClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    @CircuitBreaker(name = "customer-client", fallbackMethod = "getMainPageDataFallback")
    @Retry(name = "customer-client")
    public MainPageData getMainPageData(String login) {
    log.info("Fetching main page data for user: {}", login);
        return customerClient.getMainPage(login);
    }

    public MainPageData getMainPageDataFallback(String login, Exception ex) {
        log.error("Customer service unavailable for user: {}. Error: {}", login, ex.getMessage());
        return new MainPageData(
            login,
            "Пользователь",
            "",
            null,
            List.of(),
            List.of(),
            List.of(),
            List.of("Сервис аккаунтов временно недоступен. Попробуйте позже."),
            null,
            null,
            null,
            null
        );
    }

    public List<String> changePassword(String login, String password, String confirmPassword) {
        return customerClient.editPassword(login, new EditPasswordRequest(password, confirmPassword));
    }

    @CircuitBreaker(name = "customer-client", fallbackMethod = "registerCustomerFallback")
    @Retry(name = "customer-client")
    public CustomerRegistrationResponse registerCustomer(
            String login,
            String password,
            String confirmPassword,
            String name,
            String email,
            LocalDate birthdate
    ) {
        log.info("Registering new customer: {}", login);
        var request = new CustomerRegistrationRequest(
                login, password, confirmPassword, name, email, birthdate
        );
        return customerClient.registerCustomer(request);
    }

    public CustomerRegistrationResponse registerCustomerFallback(
            String login, String password, String confirmPassword,
            String name, String email, LocalDate birthdate, Exception ex
    ) {
        log.error("Customer registration service unavailable for user: {}. Error: {}", login, ex.getMessage());
        return new CustomerRegistrationResponse(
                false,
                List.of("Сервис регистрации временно недоступен. Попробуйте позже.")
        );
    }

    @CircuitBreaker(name = "customer-client", fallbackMethod = "editUserAccountsFallback")
    @Retry(name = "customer-client")
    public List<String> editUserAccounts(
            String login,
            String name,
            LocalDate birthdate,
            List<String> accounts
    ) {
        log.info("Editing accounts for customer: {}", login);
        var request = new EditUserAccountsRequest(name, birthdate, accounts);
        return customerClient.editUserAccounts(login, request);
    }

    public List<String> editUserAccountsFallback(
            String login, String name,
            LocalDate birthdate, List<String> accounts, Exception ex
    ) {
        log.error("Customer edit accounts service unavailable for user: {}. Error: {}", login, ex.getMessage());
        return List.of("Сервис редактирования профиля временно недоступен. Попробуйте позже.");
    }

    @CircuitBreaker(name = "exchange-client", fallbackMethod = "getExchangeRatesFallback")
    @Retry(name = "exchange-client")
    public List<ExchangeRateDto> getExchangeRates() {
        log.debug("Fetching exchange rates");
        return exchangeClient.getRates();
    }

    public List<ExchangeRateDto> getExchangeRatesFallback(Exception ex) {
        log.error("Exchange service unavailable. Using default rates. Error: {}", ex.getMessage());

        return List.of(
                new ExchangeRateDto("Рубль", "RUB", 1.0),
                new ExchangeRateDto("Доллар", "USD", 95.0),
                new ExchangeRateDto("Юань", "CNY", 13.5)
        );
    }

    public List<String> processCashOperation(String login, String currency, BigDecimal value, String action) {
        try {
            // с формы приходит action = "PUT" или "GET", такие дела..
            var cashAction = "PUT".equals(action) ? CashAction.DEPOSIT : CashAction.WITHDRAW;
            var request = new CashOperationRequest(login, currency, value, cashAction);

            var response = cashClient.processCashOperation(request);

            return response.isSuccess() ? List.of() : response.getErrors();
        } catch (Exception e) {
            log.error("Error calling cash service for user {}: {}", login, e.getMessage(), e);
            return List.of("Cash service unavailable: " + e.getMessage());
        }
    }

    @CircuitBreaker(name = "transfer-client", fallbackMethod = "processTransferOperationFallback")
    @Retry(name = "transfer-client")
    public List<String> processTransferOperation(String login, String fromCurrency, String toCurrency,
                                                 BigDecimal value, String toLogin) {
        try {
            log.info("Processing transfer for user {}: {} {} -> {} {}, to user: {}",
                     login, value, fromCurrency, value, toCurrency, toLogin);

            var request = new TransferRequest(fromCurrency, toCurrency, value, toLogin);
            var response = transferClient.transfer(login, request);

            if (response.isSuccess()) {
                return List.of();
            } else {
                List<String> allErrors = new ArrayList<>();
                allErrors.addAll(response.getTransferErrors());
                allErrors.addAll(response.getTransferOtherErrors());
                return allErrors;
            }
        } catch (Exception e) {
            log.error("Error calling transfer service for user {}: {}", login, e.getMessage(), e);
            return List.of("Transfer service unavailable: " + e.getMessage());
        }
    }

    public List<String> processTransferOperationFallback(String login, String fromCurrency, String toCurrency,
                                                         BigDecimal value, String toLogin, Exception ex) {
        log.error("Transfer service unavailable for user: {}. Error: {}", login, ex.getMessage());
        return List.of("Сервис переводов временно недоступен. Попробуйте позже.");
    }
}
