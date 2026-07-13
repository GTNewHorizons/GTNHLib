package com.gtnewhorizon.gtnhlib.space;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;

@Desugar
public record ImmutableXZ(int x, int z) implements XZAddressable {

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public int hashCode() {
        return Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.initialState(), x), z);
    }
}
