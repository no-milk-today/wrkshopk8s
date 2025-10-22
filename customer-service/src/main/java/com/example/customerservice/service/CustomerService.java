package com.example.customerservice.service;

import com.example.clients.customer.*;
import com.example.clients.customer.Currency;
import com.example.clients.fraud.FraudClient;
import com.example.clients.notification.NotificationRequest;
import com.example.customerservice.exception.AccountNotFoundException;
import com.example.customerservice.exception.CustomerNotFoundException;
import com.example.customerservice.model.Account;
import com.example.customerservice.model.Customer;
import com.example.customerservice.repository.AccountRepository;
import com.example.customerservice.repository.CustomerRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final FraudClient fraudClient;
    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;


    @CircuitBreaker(name = "fraudCheckService", fallbackMethod = "fraudCheckFallback")
    @Retry(name = "fraudCheckService")
    public CustomerRegistrationResponse registerCustomer(CustomerRegistrationRequest request) {
        List<String> validationErrors = validateRegistrationRequest(request);

        if (!validationErrors.isEmpty()) {
            return new CustomerRegistrationResponse(false, validationErrors);
        }

        Customer.CustomerBuilder builder = Customer.builder()
                .login(request.login())
                .name(request.name())
                .email(request.email())
                .birthdate(request.birthdate());
        if (request.password() != null) {
            // hash the password if it is provided
            builder.passwordHash(BCrypt.hashpw(request.password(), BCrypt.gensalt()));
        } else {
            // in OAuth scenario we do not save the password
            builder.passwordHash(null);
        }
        var customer = builder.build();

        // create default accounts for all available currencies
        var defaultAccounts = Arrays.stream(Currency.values())
                .map(currency -> Account.builder()
                        .currency(currency)
                        .balance(BigDecimal.ZERO)
                        .customer(customer)
                        .build())
                .toList();
        customer.setAccounts(defaultAccounts);
        log.debug("Created {} default accounts for user: {}",
                defaultAccounts.size(), customer.getLogin());

        // If we don’t say ‘save and FLUSH’ then the ID will be null.
        customerRepository.saveAndFlush(customer);

        var fraudCheckResponse =
                fraudClient.isFraudster(customer.getId());
        if (fraudCheckResponse.isFraudster()) {
            customerRepository.delete(customer);
            List<String> fraudErrors = new ArrayList<>();
            fraudErrors.add("Your registration is blocked due to fraud suspicion");
            return new CustomerRegistrationResponse(false, fraudErrors);
        }

        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to Bank-system...",
                        customer.getName())
        );

        kafkaTemplate.send("notification-topic", notificationRequest);
        log.info("Sent notification request to Kafka for customer: {}", customer.getId());
        return new CustomerRegistrationResponse(true, Collections.emptyList());
    }

    private List<String> validateRegistrationRequest(CustomerRegistrationRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.login() == null || request.login().trim().isEmpty()) {
            errors.add("Login cannot be empty");
        }

        if (request.name() == null || request.name().trim().isEmpty()) {
            errors.add("First and Last name are required");
        }

        if (request.email() == null || request.email().isEmpty() || !validateEmail(request.email())) {
            errors.add("Invalid e-mail");
        }

        if (request.birthdate() == null || !over18(request.birthdate())) {
            errors.add("You must be at least 18 years old");
        }

        // to fix frontUiService.registerCustomer(login, null, null, name, email, birthdate) problem
        if (request.password() != null || request.confirmPassword() != null) {
            if (request.password() == null || request.confirmPassword() == null) {
                errors.add("Password cannot be empty");
            } else if (!request.password().equals(request.confirmPassword())) {
                errors.add("Passwords do not match");
            }
        }

        if (customerRepository.existsByLogin(request.login())) {
            errors.add("A user with this login already exists");
        }

        if (customerRepository.existsByEmail(request.email())) {
            errors.add("A user with this email already exists");
        }

        return errors;
    }

    public CustomerRegistrationResponse fraudCheckFallback(CustomerRegistrationRequest request, Throwable ex) {
        log.error("Fraud check service is unavailable. Falling back for request: {}", request, ex);
        return new CustomerRegistrationResponse(false,
                List.of("Fraud service unavailable. Please try again later."));
    }

    private boolean validateEmail(String email) {
        // Простая regex-проверка
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private boolean over18(LocalDate birth) {
        return birth != null && birth.isBefore(LocalDate.now().minusYears(18));
    }

    // Агрегирующий метод для main
    public MainPageData getMainData(String login) {
        var customer = customerRepository.findByLogin(login)
                .orElseThrow(() -> new NoSuchElementException("Customer not found " + login));

        var accountsDto = Arrays.stream(Currency.values())
                .map(currency -> {
                    Optional<Account> account = customer.getAccounts() == null
                            ? Optional.empty()
                            : customer.getAccounts().stream()
                                .filter(acc -> acc.getCurrency() == currency)
                                .findFirst();
                    return new AccountDto(
                            currency,
                            currency.name(),
                            currency.getTitle(),
                            account.map(Account::getBalance).orElse(BigDecimal.ZERO),
                            account.isPresent()
                    );
                }).collect(Collectors.toList());

        // Prepare users list for transfers etc.
        var users = customerRepository.findAll().stream()
                .map(user -> new UserShortDto(user.getLogin(), user.getName()))
                .collect(Collectors.toList());

        var currencies = Arrays.stream(Currency.values())
                .map(currency -> new CurrencyDto(currency.name(), currency.getTitle()))
                .collect(Collectors.toList());

        return new MainPageData(
                customer.getLogin(),
                customer.getName(),
                customer.getEmail(),
                customer.getBirthdate(),
                accountsDto,
                currencies,
                users,
                null,
                null,
                null,
                null,
                null
        );
    }

    // CRUD для профиля и счетов (методы edit)
    public List<String> editUserProfile(String login, EditUserAccountsRequest request) {
        List<String> errors = new ArrayList<>();
        var customer = customerRepository.findByLogin(login)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));

        if (request.name() == null || request.name().trim().isEmpty()) {
            errors.add("First and Last name are required");
        }
        if (request.birthdate() == null || !over18(request.birthdate())) {
            errors.add("You must be at least 18 years old");
        }
        if (!errors.isEmpty()) return errors;

        customer.setName(request.name());
        customer.setBirthdate(request.birthdate());

        // --- Accounts ---
        Set<Currency> desiredCurrencies = request.accounts() == null
                ? Collections.emptySet()
                : request.accounts().stream().map(Currency::valueOf).collect(Collectors.toSet());

        // Duplicate check
        if (desiredCurrencies.size() != (request.accounts() == null ? 0 : request.accounts().size())) {
            errors.add("Duplicate currencies in the accounts list");
            return errors;
        }

        // Get rid of unwanted accounts (only if balance = 0)
        var accountsToRemove = customer.getAccounts().stream()
                .filter(account -> !desiredCurrencies.contains(account.getCurrency()))
                .toList();

        for (var account : accountsToRemove) {
            if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                errors.add("Cannot delete account " + account.getCurrency() + " with non-zero balance");
            }
        }

        if (!errors.isEmpty()) return errors;

        customer.getAccounts().removeAll(accountsToRemove);

        // Добавляем недостающие счета (balance = 0)
        Set<Currency> existingCurrencies = customer.getAccounts().stream()
                .map(Account::getCurrency).collect(Collectors.toSet());

        for (Currency currency : desiredCurrencies) {
            if (!existingCurrencies.contains(currency)) {
                customer.getAccounts().add(Account.builder()
                        .currency(currency)
                        .balance(BigDecimal.ZERO)
                        .customer(customer)
                        .build());
            }
        }

        customerRepository.save(customer);
        return Collections.emptyList();
    }

    public List<String> editPassword(String login, EditPasswordRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.password() == null || request.password().isEmpty()) {
            errors.add("Password cannot be empty");
        }

        if (!Objects.equals(request.password(), request.confirmPassword())) {
            errors.add("Passwords do not match");
        }

        if (!errors.isEmpty()) return errors;

        Customer customer = customerRepository.findByLogin(login)
                .orElseThrow(() -> new NoSuchElementException("Customer not found"));

        customer.setPasswordHash(BCrypt.hashpw(request.password(), BCrypt.gensalt()));
        customerRepository.save(customer);

        return Collections.emptyList();
    }

    public CustomerDto getCustomerByLogin(String login) {
        var customer = customerRepository.findByLogin(login)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + login));

        return new CustomerDto(
                customer.getId(),
                customer.getLogin(),
                customer.getName(),
                customer.getEmail(),
                customer.getBirthdate(),
                customer.getAccounts().stream()
                        .map(account -> new AccountDto(
                                account.getCurrency(),
                                account.getCurrency().getCode(),
                                account.getCurrency().getTitle(),
                                account.getBalance(),
                                true
                        ))
                        .toList()
        );
    }

    public void updateAccountBalance(String login, String currency, BigDecimal newBalance) {
        Customer customer = customerRepository.findByLogin(login)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + login));

        Account account = customer.getAccounts().stream()
                .filter(acc -> currency.equals(acc.getCurrency().getCode()))
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException("Account with currency " + currency + " not found for user " + login));

        account.setBalance(newBalance);
        customerRepository.save(customer);

        log.info("Account balance updated successfully for user {}: {} = {}", login, currency, newBalance);
    }
}
