package com.gtnewhorizon.gtnhlib.config;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

public final class SyncedConfigElement {

    private final Object instance;
    private final Field field;
    private final Runnable restore;
    private boolean synced = false;

    SyncedConfigElement(@Nullable Object instance, @NotNull Field field, @NotNull Runnable restore) {
        this.instance = instance;
        this.field = field;
        this.restore = restore;
    }

    void setSyncValue(String value) throws ConfigException {
        ConfigFieldParser.setValueFromString(instance, field, value);
        synced = true;
    }

    void restoreValue() {
        restore.run();
        synced = false;
    }

    boolean isSynced() {
        return synced;
    }

    public String getValue() throws ConfigException {
        return ConfigFieldParser.getValueAsString(instance, field);
    }

    @Override
    public String toString() {
        return field.toString();
    }
}
