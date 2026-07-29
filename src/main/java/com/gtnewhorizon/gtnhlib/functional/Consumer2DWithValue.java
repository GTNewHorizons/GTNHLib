package com.gtnewhorizon.gtnhlib.functional;

@FunctionalInterface
public interface Consumer2DWithValue<T> {

    void accept(int posX, int posZ, T value);
}
