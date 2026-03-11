package com.gtnewhorizon.gtnhlib.client.ResourcePackDarkModeFix;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public final class DarkModeFixColorProcessor {

    private static final Int2IntOpenHashMap COLOR_CACHE = new Int2IntOpenHashMap();
    private static int debugLogRemaining = 5;
    private static boolean infoLoggedOnce = false;
    private static boolean callLoggedOnce = false;
    private static int debugSampleRemaining = 5;
    private static boolean containerStateLoggedOnce = false;

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
        if (!DarkModeFixController.enabled) {
            return color;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen == null) {
            return color;
        }
        if (mc.currentScreen instanceof GuiChat) {
            return color;
        }
        if (!(DarkModeFixController.inGuiScreen || DarkModeFixController.inContainerGui)) {
            return color;
        }
        if (DarkModeFixController.inTooltip || DarkModeFixController.inItemOverlay || DarkModeFixController.inChat) {
            return color;
        }
        DarkModeFixConfig config = DarkModeFixController.config;
        if (config == null) {
            return color;
        }
        if (!callLoggedOnce) {
            callLoggedOnce = true;
            GTNHLib.LOG.info("DarkModeFix processing GUI text color (first call this session)");
        }
        if (!containerStateLoggedOnce && DarkModeFixController.inContainerGui) {
            containerStateLoggedOnce = true;
            GTNHLib.LOG.info(
                    "DarkModeFix container call: inGuiScreen={}, inTooltip={}, inItemOverlay={}",
                    DarkModeFixController.inGuiScreen,
                    DarkModeFixController.inTooltip,
                    DarkModeFixController.inItemOverlay);
        }
        int alpha = (color >> 24) & 255;
        if (alpha == 0 && !allowZeroAlpha) {
            logSample(color, color, 0.0f, "alpha_zero_skip");
            return color;
        }

        int cached = COLOR_CACHE.get(color);
        if (cached != 0 || COLOR_CACHE.containsKey(color)) {
            logSample(color, cached, 0.0f, "cache_hit");
            return cached;
        }

        int rgb = color & 0xFFFFFF;
        if (rgb == 0x000000) {
            int adjusted = (alpha << 24) | config.minGrayColor;
            COLOR_CACHE.put(color, adjusted);
            return adjusted;
        }
        if (rgb == 0xFFFFFF) {
            int adjusted = (alpha << 24) | config.maxGrayColor;
            COLOR_CACHE.put(color, adjusted);
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
            logSample(color, color, brightness, "within_threshold");
            return color;
        }

        int adjusted;
        if (red == green && green == blue) {
            float gray = targetBrightness;
            int grayInt = toByte(gray);
            adjusted = (alpha << 24) | (grayInt << 16) | (grayInt << 8) | grayInt;
        } else {
            float factor = brightness <= 0.0001f ? 1.0f : targetBrightness / brightness;
            float r2 = clamp01(r * factor);
            float g2 = clamp01(g * factor);
            float b2 = clamp01(b * factor);
            adjusted = (alpha << 24) | (toByte(r2) << 16) | (toByte(g2) << 8) | toByte(b2);
        }

        COLOR_CACHE.put(color, adjusted);
        logAdjustment(color, adjusted);
        logSample(color, adjusted, brightness, "adjusted");
        return adjusted;
    }

    public static void clearCache() {
        COLOR_CACHE.clear();
        debugLogRemaining = 5;
        infoLoggedOnce = false;
        callLoggedOnce = false;
        debugSampleRemaining = 5;
        containerStateLoggedOnce = false;
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static int toByte(float value) {
        int out = Math.round(value * 255.0f);
        if (out < 0) {
            return 0;
        }
        if (out > 255) {
            return 255;
        }
        return out;
    }

    private static void logAdjustment(int originalColor, int adjustedColor) {
        if (debugLogRemaining <= 0) {
            return;
        }
        debugLogRemaining--;
        GTNHLib.LOG.info(
                "DarkModeFix adjustColor: 0x{} -> 0x{}",
                Integer.toHexString(originalColor),
                Integer.toHexString(adjustedColor));
        if (!infoLoggedOnce) {
            infoLoggedOnce = true;
            GTNHLib.LOG.info("DarkModeFix adjusted GUI text color (first occurrence this session)");
        }
    }

    private static void logSample(int originalColor, int adjustedColor, float brightness, String reason) {
        if (debugSampleRemaining <= 0) {
            return;
        }
        debugSampleRemaining--;
        GTNHLib.LOG.info(
                "DarkModeFix sample: in=0x{}, out=0x{}, brightness={}, reason={}",
                Integer.toHexString(originalColor),
                Integer.toHexString(adjustedColor),
                brightness,
                reason);
    }

}
