package com.example.customerservice.repository;

import com.example.customerservice.model.Account;
import com.example.customerservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Currency;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    List<Account> findAllByCustomer(Customer customer);
    boolean existsByCustomerAndCurrency(Customer customer, Currency currency);
}
