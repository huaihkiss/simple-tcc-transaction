package com.huaihkiss.tcc.exception;

public class GlobalTransationException extends RuntimeException {
    public GlobalTransationException() {
    }

    public GlobalTransationException(String message) {
        super(message);
    }

    public GlobalTransationException(String message, Throwable cause) {
        super(message, cause);
    }

    public GlobalTransationException(Throwable cause) {
        super(cause);
    }

    public GlobalTransationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
