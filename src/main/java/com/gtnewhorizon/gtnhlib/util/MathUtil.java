package com.gtnewhorizon.gtnhlib.util;

public class MathUtil {

    /// I think this does the [Math#floorMod(int, int)] thing to floats. I'm sure it's implemented *somewhere*, but I
    /// don't know where.
    public static float mod(float a, float b) {
        if (a < 0) {
            final var ret = b - (-a % b);
            return ret == b ? 0 : ret; // when -a == b, should return 0, not b
        }
        return a % b;
    }
}
