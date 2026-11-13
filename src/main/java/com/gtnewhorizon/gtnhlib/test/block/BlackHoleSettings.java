package com.gtnewhorizon.gtnhlib.test.block;

// Changed live by /blackhole set. Lengths other than the radius are fractions of it.
final class BlackHoleSettings {

    // In blocks.
    static double radius = 8;
    static float horizon = 0.08f;
    // Bending fades out between these, so rays leave the sphere straight and line up with the world around it.
    static float fadeStart = 0.6f;
    static float fadeEnd = 1.0f;
    static int captureSize = 512;
    // Faces per second, across all holes.
    static double captureRate = 6;
    static int version;

    private BlackHoleSettings() {}
}
