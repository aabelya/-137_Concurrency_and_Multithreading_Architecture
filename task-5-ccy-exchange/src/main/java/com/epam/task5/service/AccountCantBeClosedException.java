package com.epam.task5.service;

import com.epam.task5.ExchangeAppException;

public class AccountCantBeClosedException extends ExchangeAppException {

    public AccountCantBeClosedException() {
    }

    public AccountCantBeClosedException(String message) {
        super(message);
    }

    public AccountCantBeClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountCantBeClosedException(Throwable cause) {
        super(cause);
    }

    public AccountCantBeClosedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
