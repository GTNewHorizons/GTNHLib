package com.gtnewhorizon.gtnhlib.space;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker2D;

@Desugar
public record ImmutableXZ(int x, int z) implements XZAddressable {

    public static ImmutableXZ unpack(long coord) {
        return new ImmutableXZ(CoordinatePacker2D.unpackChunkX(coord), CoordinatePacker2D.unpackChunkZ(coord));
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
    public int hashCode() {
        return Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.initialState(), x), z);
    }
}
