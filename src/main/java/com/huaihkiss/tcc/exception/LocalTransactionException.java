package com.huaihkiss.tcc.exception;

public class LocalTransactionException  extends RuntimeException {
    public LocalTransactionException() {
    }

    public LocalTransactionException(String message) {
        super(message);
    }

    public LocalTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public LocalTransactionException(Throwable cause) {
        super(cause);
    }

    public LocalTransactionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
