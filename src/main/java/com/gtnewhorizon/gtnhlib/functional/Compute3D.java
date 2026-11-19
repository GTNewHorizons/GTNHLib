package com.gtnewhorizon.gtnhlib.functional;

@FunctionalInterface
public interface Compute3D<V> {

    V apply(int posX, int posY, int posZ);
}
