package com.gtnewhorizon.gtnhlib.config;

/**
 * A really basic wrapper for config to simplify handling them in external code.
 */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(Throwable cause) {
        super(cause);
    }
}
