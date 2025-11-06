package com.gtnewhorizon.gtnhlib.client.renderer.cel.util;

public class MathUtil {

    public static boolean roughlyEqual(float a, float b) {
        return Math.abs(b - a) < 0.00001f;
    }
}
