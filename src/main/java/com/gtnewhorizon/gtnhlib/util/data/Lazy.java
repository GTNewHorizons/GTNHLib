package com.gtnewhorizon.gtnhlib.util.data;

import java.util.function.Supplier;

/**
 * Just a container that will lazy-load a value. Can be used in the place of a Supplier<T>. This is useful if you want a
 * static final field that shouldn't be initialized when the class is loaded.
 */
public class Lazy<T> implements Supplier<T> {

    private boolean hasValue = false;
    private T value;

    private Supplier<T> getter;

    public Lazy(Supplier<T> getter) {
        this.getter = getter;
    }

    /**
     * Sets the value even if it's been initialized already.
     */
    public synchronized void set(T value) {
        hasValue = true;
        this.value = value;
        getter = null;
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

    public synchronized boolean hasValue() {
        return hasValue;
    }

    /**
     * Initializes the value. {@link #get()}, but with a more readable method name.
     */
    public void initialize() {
        get();
    }
}
