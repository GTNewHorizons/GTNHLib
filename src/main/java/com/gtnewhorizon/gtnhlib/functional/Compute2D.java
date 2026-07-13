package com.gtnewhorizon.gtnhlib.functional;

@FunctionalInterface
public interface Compute2D<V> {

    V apply(int posX, int posZ);
}
