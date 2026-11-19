package com.gtnewhorizon.gtnhlib.functional;

@FunctionalInterface
public interface Consumer3DWithValue<T> {

    void accept(int posX, int posY, int posZ, T value);
}
