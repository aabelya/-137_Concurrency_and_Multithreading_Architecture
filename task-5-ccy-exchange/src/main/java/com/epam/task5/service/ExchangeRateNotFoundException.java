package com.epam.task5.service;

import com.epam.task5.ExchangeAppException;

import java.util.Currency;

public class ExchangeRateNotFoundException extends ExchangeAppException {

    public ExchangeRateNotFoundException(Currency ccy1, Currency ccy2) {
        super(String.format("%s -> %s", ccy1, ccy2));
    }

    public ExchangeRateNotFoundException() {
    }

    public ExchangeRateNotFoundException(String message) {
        super(message);
    }

    public ExchangeRateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExchangeRateNotFoundException(Throwable cause) {
        super(cause);
    }

    public ExchangeRateNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
