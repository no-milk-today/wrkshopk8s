package com.example.clients.customer;

public enum Currency {
    RUB("RUB", "Рубль"),
    USD("USD", "Доллар"),
    CNY("CNY", "Юань");

    private final String code;
    private final String title;

    Currency(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
