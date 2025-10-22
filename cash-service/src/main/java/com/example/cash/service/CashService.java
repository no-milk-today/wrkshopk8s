package com.example.cash.service;

import com.example.clients.cash.CashAction;
import com.example.clients.cash.CashOperationRequest;
import com.example.clients.cash.CashOperationResponse;
import com.example.clients.customer.AccountDto;
import com.example.clients.customer.CustomerClient;
import com.example.clients.customer.CustomerDto;
import com.example.clients.notification.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashService {

    private final CustomerClient customerClient;
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

    /**
     * Обработка операции с деньгами (пополнение или снятие)
     */
    public CashOperationResponse processCashOperation(CashOperationRequest request) {
        log.info("Processing cash operation for user {}: {} {} {}",
                request.login(), request.action(), request.value(), request.currency());

        try {
            // 1. Валидация запроса
            List<String> errors = validateRequest(request);
            if (!errors.isEmpty()) {
                log.warn("Cash operation validation failed for user {}: {}", request.login(), errors);
                return CashOperationResponse.error(errors);
            }

            // 2. Получаем данные пользователя
            CustomerDto customer = customerClient.getCustomer(request.login());
            if (customer == null) {
                log.error("Customer not found: {}", request.login());
                return CashOperationResponse.error("Customer not found");
            }

            // 3. Проверяем наличие счета с указанной валютой
            AccountDto account = customer.findAccountByCurrency(request.currency());
            if (account == null) {
                log.error("Account with currency {} not found for user {}", request.currency(), request.login());
                return CashOperationResponse.error("Account with currency " + request.currency() + " not found");
            }

            // 4. Для операций снятия проверяем достаточность средств
            if (request.action() == CashAction.WITHDRAW) {
                if (account.balance().compareTo(request.value()) < 0) {
                    log.warn("Insufficient funds for withdrawal. User: {}, requested: {} {}, available: {} {}",
                            request.login(), request.value(), request.currency(), account.balance(), request.currency());
                    return CashOperationResponse.error(
                            String.format("Недостаточно средств. Доступно: %s %s",
                                    account.balance(), request.currency()));
                }
            }

            // 5. Рассчитываем новый баланс
            BigDecimal newBalance = calculateNewBalance(account.balance(), request.value(), request.action());

            // 6. Обновляем баланс в Customer сервисе
            customerClient.updateAccountBalance(request.login(), request.currency(), newBalance);

            // 7. Отправляем уведомление пользователю
            sendNotification(customer, request, newBalance);

            log.info("Cash operation completed successfully for user {}: {} {} {}. New balance: {} {}",
                    request.login(), request.action(), request.value(), request.currency(), newBalance, request.currency());

            return CashOperationResponse.success();

        } catch (Exception e) {
            log.error("Error during cash operation for user {}: {}", request.login(), e.getMessage(), e);
            return CashOperationResponse.error("Внутренняя ошибка сервера. Попробуйте позже.");
        }
    }

    /**
     * Валидация запроса на операцию с деньгами
     */
    private List<String> validateRequest(CashOperationRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Запрос не может быть пустым");
            return errors;
        }

        if (request.login() == null || request.login().trim().isEmpty()) {
            errors.add("Логин пользователя обязателен");
        }

        if (request.currency() == null || request.currency().trim().isEmpty()) {
            errors.add("Валюта обязательна");
        }

        if (request.value() == null) {
            errors.add("Сумма операции обязательна");
        } else if (request.value().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Сумма операции должна быть больше нуля");
        }

        if (request.action() == null) {
            errors.add("Тип операции обязателен");
        }

        // Проверка поддерживаемых валют
        if (request.currency() != null && !isSupportedCurrency(request.currency())) {
            errors.add("Неподдерживаемая валюта: " + request.currency());
        }

        return errors;
    }

    /**
     * Проверка поддерживаемых валют
     */
    private boolean isSupportedCurrency(String currency) {
        return List.of("RUB", "USD", "CNY").contains(currency.toUpperCase());
    }

    /**
     * Расчет нового баланса в зависимости от операции
     */
    private BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal operationValue, CashAction action) {
        return switch (action) {
            case DEPOSIT -> currentBalance.add(operationValue);
            case WITHDRAW -> currentBalance.subtract(operationValue);
        };
    }

    /**
     * Отправка уведомления пользователю об операции
     */
    private void sendNotification(CustomerDto customer, CashOperationRequest request, BigDecimal newBalance) {
        try {
            String actionText = request.action() == CashAction.DEPOSIT ? "пополнение" : "снятие";
            String message = String.format(
                    "Выполнено %s счета на сумму %s %s. Новый баланс: %s %s",
                    actionText, request.value(), request.currency(), newBalance, request.currency()
            );

            NotificationRequest notificationRequest = new NotificationRequest(
                    customer.id(),
                    customer.email(),
                    message
            );

            kafkaTemplate.send("notification-topic", notificationRequest);
            log.info("Notification sent to user {} about cash operation", customer.login());
        } catch (Exception e) {
            log.warn("Failed to send notification to user {}: {}", customer.login(), e.getMessage());
            // Не прерываем операцию из-за ошибки отправки уведомления
        }
    }
}
