package com.gtnewhorizon.gtnhlib.space;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;

@Desugar
public record ImmutableXYZ(int x, int y, int z) implements XYZAddressable {

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
    public int hashCode() {
        return Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.initialState(), x), y), z);
    }
}
