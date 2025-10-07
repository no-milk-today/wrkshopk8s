package com.example.clients.transfer;

import lombok.Data;

import java.util.List;

@Data
public class TransferResponse {
    private boolean success;
    private List<String> transferErrors;
    private List<String> transferOtherErrors;

    public TransferResponse(boolean success, List<String> transferErrors, List<String> transferOtherErrors) {
        this.success = success;
        this.transferErrors = transferErrors;
        this.transferOtherErrors = transferOtherErrors;
    }

    // Пустой конструктор для Jackson
    public TransferResponse() {}

    // Статические фабрики
    public static TransferResponse success() {
        return new TransferResponse(true, List.of(), List.of());
    }

    public static TransferResponse error(String errorMessage) {
        return new TransferResponse(false, List.of(errorMessage), List.of());
    }

    public static TransferResponse errors(List<String> transferErrors) {
        return new TransferResponse(false, transferErrors, List.of());
    }

    public static TransferResponse otherError(String otherError) {
        return new TransferResponse(false, List.of(), List.of(otherError));
    }

    public static TransferResponse otherErrors(List<String> otherErrors) {
        return new TransferResponse(false, List.of(), otherErrors);
    }

    public static TransferResponse fullError(List<String> transferErrors, List<String> transferOtherErrors) {
        return new TransferResponse(false, transferErrors, transferOtherErrors);
    }
}
