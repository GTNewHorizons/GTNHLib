package com.gtnewhorizon.gtnhlib.blockstate.core;

public class InvalidPropertyTextException extends RuntimeException {

    public InvalidPropertyTextException(String message) {
        super(message);
    }

    public InvalidPropertyTextException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertyTextException(Throwable cause) {
        super(cause);
    }

    public InvalidPropertyTextException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
