package com.gtnewhorizon.gtnhlib.util.data;

import java.util.function.Supplier;

/**
 * Just a container that will lazy-load a value. Can be used in the place of a Supplier<T>
 */
public class Lazy<T> implements Supplier<T> {

    private boolean hasValue = false;
    private T value;

    private Supplier<T> getter;

    public Lazy(Supplier<T> getter) {
        this.getter = getter;
    }

    /**
     * Gets the value. Thread safe.
     */
    @Override
    public synchronized T get() {
        if (!hasValue) {
            value = getter.get();
            getter = null;
            hasValue = true;
        }

        return value;
    }
}
