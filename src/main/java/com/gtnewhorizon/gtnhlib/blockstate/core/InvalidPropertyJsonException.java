package com.gtnewhorizon.gtnhlib.blockstate.core;

public class InvalidPropertyJsonException extends RuntimeException {

    public InvalidPropertyJsonException(String message) {
        super(message);
    }

    public InvalidPropertyJsonException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertyJsonException(Throwable cause) {
        super(cause);
    }

    public InvalidPropertyJsonException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
