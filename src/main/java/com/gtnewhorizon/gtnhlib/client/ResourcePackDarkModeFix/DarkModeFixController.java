package com.gtnewhorizon.gtnhlib.client.ResourcePackDarkModeFix;

import com.gtnewhorizon.gtnhlib.GTNHLib;

public class DarkModeFixController {

    public static boolean enabled;
    public static boolean inContainerGui;
    public static DarkModeFixConfig config;

    public static void enable(DarkModeFixConfig config) {
        DarkModeFixController.config = config;
        enabled = true;
        log("DarkModeFix enabled");
    }

    public static void disable() {
        enabled = false;
        inContainerGui = false;
        config = null;
        log("DarkModeFix disabled");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void clearColorCache() {
        DarkModeFixColorProcessor.clearCache();
    }

    public static void setInContainerGui(boolean value) {
        inContainerGui = value;
    }

    private static void log(String message, Object... args) {
        GTNHLib.LOG.debug("[DarkModeFix] " + message, args);
    }
}
