package com.gtnewhorizon.gtnhlib.space;

import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;

public class MutableXZ implements XZAddressable {

    public int x, z;

    public MutableXZ() {}

    public MutableXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof MutableXZ mutableXZ)) return false;

        return x == mutableXZ.x && z == mutableXZ.z;
    }

    @Override
    public int hashCode() {
        return Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.initialState(), x), z);
    }

    @Override
    public String toString() {
        return "MutableXZ{" + "x=" + x + ", z=" + z + '}';
    }
}
