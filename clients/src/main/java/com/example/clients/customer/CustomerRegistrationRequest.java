package com.example.clients.customer;

import java.time.LocalDate;

public record CustomerRegistrationRequest(
        String login,
        String password,
        String confirmPassword,
        String name,
        String email,
        LocalDate birthdate) {

}
