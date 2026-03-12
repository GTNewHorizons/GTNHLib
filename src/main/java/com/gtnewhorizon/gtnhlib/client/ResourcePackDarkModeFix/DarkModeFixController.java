package com.gtnewhorizon.gtnhlib.client.ResourcePackDarkModeFix;

public class DarkModeFixController {

    public static boolean enabled;
    public static boolean inContainerGui;
    public static DarkModeFixConfig config;

    public static void enable(DarkModeFixConfig config) {
        DarkModeFixController.config = config;
        enabled = true;
    }

    public static void disable() {
        enabled = false;
        inContainerGui = false;
        config = null;
    }

    public static void clearColorCache() {
        DarkModeFixColorProcessor.clearCache();
    }

    public static void setInContainerGui(boolean value) {
        inContainerGui = value;
    }
}
