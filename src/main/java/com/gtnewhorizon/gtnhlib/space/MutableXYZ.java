package com.gtnewhorizon.gtnhlib.space;

import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;

public class MutableXYZ implements XYZAddressable {

    public int x, y, z;

    public MutableXYZ() {}

    public MutableXYZ(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof MutableXYZ that)) return false;

        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.initialState(), x), y), z);
    }

    @Override
    public String toString() {
        return "MutableXYZ{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}
