package com.epam.task5;

public class OperationInterruptedException extends RuntimeException {
    public OperationInterruptedException() {
    }

    public OperationInterruptedException(String message) {
        super(message);
    }

    public OperationInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationInterruptedException(Throwable cause) {
        super(cause);
    }

    public OperationInterruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
