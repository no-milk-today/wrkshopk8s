package com.example.clients.cash;

import lombok.Data;

import java.util.List;


@Data
public class CashOperationResponse {
    private boolean success;
    private List<String> errors;

    public CashOperationResponse(boolean success, List<String> errors) {
        this.success = success;
        this.errors = errors;
    }

    // Default constructor для Jackson
    public CashOperationResponse() {}

    public static CashOperationResponse success() {
        return new CashOperationResponse(true, List.of());
    }

    public static CashOperationResponse error(String errorMessage) {
        return new CashOperationResponse(false, List.of(errorMessage));
    }

    public static CashOperationResponse error(List<String> errors) {
        return new CashOperationResponse(false, errors);
    }
}
