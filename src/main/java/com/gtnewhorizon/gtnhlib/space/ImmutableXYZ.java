package com.gtnewhorizon.gtnhlib.space;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackX;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackY;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackZ;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;

@Desugar
public record ImmutableXYZ(int x, int y, int z) implements XYZAddressable {

    public static ImmutableXYZ unpack(long coord) {
        return new ImmutableXYZ(unpackX(coord), unpackY(coord), unpackZ(coord));
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
    public int hashCode() {
        return Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.hashStep(Fnv1a32.initialState(), x), y), z);
    }
}
