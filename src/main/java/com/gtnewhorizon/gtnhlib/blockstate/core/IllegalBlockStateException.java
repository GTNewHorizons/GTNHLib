package com.gtnewhorizon.gtnhlib.blockstate.core;

/// Represents an illegal operation on a [BlockState].
/// This exception is frequently instantiated and passed as the final argument to
/// {@code GTNHLib.LOG.warn(..., new IllegalBlockStateException())} so that SLF4J captures a stack trace at
/// the call site without actually throwing. The exception is never thrown in normal code paths.
public class IllegalBlockStateException extends RuntimeException {

    public IllegalBlockStateException() {}

    public IllegalBlockStateException(String message) {
        super(message);
    }

    public IllegalBlockStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalBlockStateException(Throwable cause) {
        super(cause);
    }

    public IllegalBlockStateException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
