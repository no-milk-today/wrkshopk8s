package com.example.transfer.service;

import com.example.clients.customer.AccountDto;
import com.example.clients.customer.CustomerClient;
import com.example.clients.customer.CustomerDto;
import com.example.clients.exchange.ExchangeClient;
import com.example.clients.exchange.ExchangeRateDto;
import com.example.clients.fraud.FraudClient;
import com.example.clients.notification.NotificationRequest;
import com.example.clients.transfer.TransferRequest;
import com.example.clients.transfer.TransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final CustomerClient customerClient;
    private final FraudClient fraudClient;
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;
    private final ExchangeClient exchangeClient;

    public TransferResponse transfer(String login, TransferRequest req) {

        List<String> ownErrors = new ArrayList<>();
        List<String> otherErrors = new ArrayList<>();

        var sender = customerClient.getCustomer(login);

        if (fraudClient.isFraudster(sender.id()).isFraudster()) {
            ownErrors.add("Fraud detected");
            return TransferResponse.errors(ownErrors);
        }

        var receiver = login.equals(req.toLogin())
                ? sender
                : customerClient.getCustomer(req.toLogin());

        var fromAcc = find(sender, req.fromCurrency());
        var toAcc = find(receiver, req.toCurrency());

        if (fromAcc.isEmpty()) {
            ownErrors.add("Source account not found");
        }
        if (toAcc.isEmpty()) {
            (login.equals(req.toLogin()) ? ownErrors : otherErrors)
                    .add("Destination account not found");
        }
        if (!ownErrors.isEmpty() || !otherErrors.isEmpty()) {
            return buildError(ownErrors, otherErrors);
        }

        var debitAmount = req.value();

        BigDecimal creditAmount;
        try {
            creditAmount = convertCurrency(debitAmount, req.fromCurrency(), req.toCurrency());
        } catch (Exception e) {
            log.error("Currency conversion failed: {}", e.getMessage(), e);
            ownErrors.add("Currency conversion failed: " + e.getMessage());
            return TransferResponse.errors(ownErrors);
        }

        // Проверка достаточности средств (всегда по исходной валюте)
        if (fromAcc.get().balance().compareTo(debitAmount) < 0) {
            ownErrors.add("Insufficient funds");
            return TransferResponse.errors(ownErrors);
        }

        if (!ownErrors.isEmpty() || !otherErrors.isEmpty()) {
            return buildError(ownErrors, otherErrors);
        }

        /* Обновляем балансы */
        // todo: make it transactional
        customerClient.updateAccountBalance(
                login, req.fromCurrency(),
                fromAcc.get().balance().subtract(debitAmount));

        customerClient.updateAccountBalance(
                receiver.login(), req.toCurrency(),
                toAcc.get().balance().add(creditAmount));

        /* Notification */
                    var msg = String.format(
                            "Перевод %.2f %s -> %.2f %s от %s к %s",
                            debitAmount, req.fromCurrency(),
                            creditAmount, req.toCurrency(),
                            sender.login(), login.equals(req.toLogin()) ? sender.login() : receiver.login());
        kafkaTemplate.send("notification-topic", new NotificationRequest(sender.id(), sender.name(), msg));
        if (!sender.login().equals(receiver.login())) {
            kafkaTemplate.send("notification-topic", new NotificationRequest(receiver.id(), receiver.name(), msg));
        }

        return TransferResponse.success();
    }

    /* helpers */
    private static Optional<AccountDto> find(CustomerDto c, String code) {
        return c.accounts().stream()
                .filter(a -> a.currency().getCode().equals(code))
                .findFirst();
    }

    private static TransferResponse buildError(
            List<String> own, List<String> other) {

        if (!own.isEmpty() && !other.isEmpty()) {
            return TransferResponse.fullError(own, other);
        }
        if (!own.isEmpty()) {
            return TransferResponse.errors(own);
        }
        return TransferResponse.otherErrors(other);
    }

    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        List<ExchangeRateDto> rates = exchangeClient.getRates();
        double fromRate = getRateValue(rates, fromCurrency);
        double toRate = getRateValue(rates, toCurrency);

        var fromRateBD = BigDecimal.valueOf(fromRate);
        var toRateBD = BigDecimal.valueOf(toRate);

        return amount.multiply(fromRateBD)
                .divide(toRateBD, 0, RoundingMode.HALF_UP);  // 0 decimal places
    }


    /**
     * Получает курс валюты к рублю
     */
    private double getRateValue(List<ExchangeRateDto> rates, String currency) {
        if ("RUB".equals(currency)) {
            return 1.0; // Рубль - базовая валюта
        }

        return rates.stream()
                .filter(rate -> rate.getName().equals(currency))
                .findFirst()
                .map(ExchangeRateDto::getValue)
                .orElseThrow(() -> new IllegalArgumentException("Exchange rate not found for " + currency));
    }
}
