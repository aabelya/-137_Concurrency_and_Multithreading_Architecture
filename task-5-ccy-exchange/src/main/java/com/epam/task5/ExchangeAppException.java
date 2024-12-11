package com.epam.task5;

public class ExchangeAppException extends Exception {

    public ExchangeAppException() {
    }

    public ExchangeAppException(String message) {
        super(message);
    }

    public ExchangeAppException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExchangeAppException(Throwable cause) {
        super(cause);
    }

    public ExchangeAppException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
