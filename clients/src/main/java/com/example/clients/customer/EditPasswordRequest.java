package com.example.clients.customer;

public record EditPasswordRequest(
        String password,
        String confirmPassword
) {}
