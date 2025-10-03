package com.example.customerservice.repository;

import com.example.customerservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByLogin(String login);
    Optional<Customer> findByEmail(String email);
    boolean existsByLogin(String login);
    boolean existsByEmail(String email);
}
