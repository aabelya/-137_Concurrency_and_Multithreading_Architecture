package com.epam.task5.service;

import com.epam.task5.ExchangeAppException;

public class IllegalExchangeRateException extends ExchangeAppException {

    public IllegalExchangeRateException() {
    }

    public IllegalExchangeRateException(String message) {
        super(message);
    }

    public IllegalExchangeRateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalExchangeRateException(Throwable cause) {
        super(cause);
    }

    public IllegalExchangeRateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
