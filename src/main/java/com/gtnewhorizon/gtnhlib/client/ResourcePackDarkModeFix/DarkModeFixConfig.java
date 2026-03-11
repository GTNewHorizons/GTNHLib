package com.gtnewhorizon.gtnhlib.client.ResourcePackDarkModeFix;

public class DarkModeFixConfig {

    public final float darkThreshold;
    public final float minBrightness;
    public final float maxBrightness;

    public final int minGrayColor;
    public final int maxGrayColor;

    public DarkModeFixConfig(float darkThreshold, float minBrightness, float maxBrightness) {
        validateBrightness("darkThreshold", darkThreshold);
        validateBrightness("minBrightness", minBrightness);
        validateBrightness("maxBrightness", maxBrightness);

        if (minBrightness > maxBrightness) {
            throw new IllegalArgumentException("minBrightness must be <= maxBrightness");
        }

        this.darkThreshold = darkThreshold;
        this.minBrightness = minBrightness;
        this.maxBrightness = maxBrightness;

        this.minGrayColor = toGrayColor(minBrightness);
        this.maxGrayColor = toGrayColor(maxBrightness);
    }

    private static void validateBrightness(String name, float value) {
        if (value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }

    private static int toGrayColor(float brightness) {
        int gray = Math.round(brightness * 255.0f);
        return (gray << 16) | (gray << 8) | gray;
    }
}
