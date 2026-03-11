package com.gtnewhorizon.gtnhlib.client.ResourcePackDarkModeFix;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public final class DarkModeFixColorProcessor {

    private static final Int2IntOpenHashMap COLOR_CACHE = new Int2IntOpenHashMap();
    private static int debugHitsRemaining = 20;
    private static int debugAdjustRemaining = 20;

    static {
        COLOR_CACHE.defaultReturnValue(Integer.MIN_VALUE);
    }

    private DarkModeFixColorProcessor() {}

    public static int adjustColor(int color) {
        return adjustColorInternal(color, false);
    }

    public static int adjustColorOpaque(int color) {
        if ((color & 0xFF000000) == 0) {
            color |= 0xFF000000;
        }
        return adjustColorInternal(color, true);
    }

    private static int adjustColorInternal(int color, boolean allowZeroAlpha) {
        if (!DarkModeFixController.enabled || !DarkModeFixController.inContainerGui) {
            return color;
        }

        DarkModeFixConfig config = DarkModeFixController.config;
        if (config == null) {
            return color;
        }

        if (debugHitsRemaining > 0) {
            debugHitsRemaining--;
            GTNHLib.LOG.info(
                    "[DarkModeFix] hit: color=0x{}, allowZeroAlpha={}, threshold={}, min={}, max={}",
                    Integer.toHexString(color),
                    allowZeroAlpha,
                    config.darkThreshold,
                    config.minBrightness,
                    config.maxBrightness);
        }

        int alpha = (color >> 24) & 255;
        if (alpha == 0 && !allowZeroAlpha) {
            return color;
        }

        int cached = COLOR_CACHE.get(color);
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }

        int rgb = color & 0xFFFFFF;

        if (rgb == 0x000000) {
            int adjusted = (alpha << 24) | config.minGrayColor;
            COLOR_CACHE.put(color, adjusted);
            if (debugAdjustRemaining > 0) {
                debugAdjustRemaining--;
                GTNHLib.LOG.info(
                        "[DarkModeFix] adjusted black: in=0x{}, out=0x{}",
                        Integer.toHexString(color),
                        Integer.toHexString(adjusted));
            }
            return adjusted;
        }

        if (rgb == 0xFFFFFF) {
            int adjusted = (alpha << 24) | config.maxGrayColor;
            COLOR_CACHE.put(color, adjusted);
            if (debugAdjustRemaining > 0) {
                debugAdjustRemaining--;
                GTNHLib.LOG.info(
                        "[DarkModeFix] adjusted white: in=0x{}, out=0x{}",
                        Integer.toHexString(color),
                        Integer.toHexString(adjusted));
            }
            return adjusted;
        }

        int red = (rgb >> 16) & 255;
        int green = (rgb >> 8) & 255;
        int blue = rgb & 255;

        float r = red / 255.0f;
        float g = green / 255.0f;
        float b = blue / 255.0f;

        float brightness = 0.2126f * r + 0.7152f * g + 0.0722f * b;

        float targetBrightness;

        if (brightness < config.darkThreshold) {
            targetBrightness = config.minBrightness;
        } else if (brightness > config.maxBrightness) {
            targetBrightness = config.maxBrightness;
        } else {
            COLOR_CACHE.put(color, color);
            return color;
        }

        int adjusted;

        if (red == green && green == blue) {
            int gray = toByte(targetBrightness);
            adjusted = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
        } else {
            float factor = brightness <= 0.0001f ? 1.0f : targetBrightness / brightness;

            float r2 = clamp01(r * factor);
            float g2 = clamp01(g * factor);
            float b2 = clamp01(b * factor);

            adjusted = (alpha << 24) | (toByte(r2) << 16) | (toByte(g2) << 8) | toByte(b2);
        }

        COLOR_CACHE.put(color, adjusted);

        if (debugAdjustRemaining > 0) {
            debugAdjustRemaining--;
            GTNHLib.LOG.info(
                    "[DarkModeFix] adjusted: in=0x{}, out=0x{}, brightness={}",
                    Integer.toHexString(color),
                    Integer.toHexString(adjusted),
                    brightness);
        }

        return adjusted;
    }

    public static void clearCache() {
        COLOR_CACHE.clear();
        debugHitsRemaining = 20;
        debugAdjustRemaining = 20;
    }

    private static float clamp01(float value) {
        if (value < 0.0f) return 0.0f;
        if (value > 1.0f) return 1.0f;
        return value;
    }

    private static int toByte(float value) {
        int out = Math.round(value * 255.0f);
        if (out < 0) return 0;
        if (out > 255) return 255;
        return out;
    }
}
