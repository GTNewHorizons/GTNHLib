package com.gtnewhorizon.gtnhlib.util;

public class MathUtil {

    /// I think this does the [Math#floorMod(int, int)] thing to floats. I'm sure it's implemented *somewhere*, but I
    /// don't know where.
    public static float mod(float a, float b) {
        if (a < 0) return b - (-a % b);
        return a % b;
    }
}
