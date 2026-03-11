package com.gtnewhorizon.gtnhlib.client.ResourcePackDarkModeFix;

import com.gtnewhorizon.gtnhlib.GTNHLib;

public class DarkModeFixController {

    public static boolean enabled;
    public static boolean inContainerGui;
    public static boolean inGuiScreen;
    public static boolean inTooltip;
    public static boolean inItemOverlay;
    public static boolean inChat;
    public static DarkModeFixConfig config;
    private static boolean containerFlagLoggedOnce = false;

    public static void enable(DarkModeFixConfig config) {
        DarkModeFixController.config = config;
        enabled = true;
        log("DarkModeFix enabled");
    }

    public static void disable() {
        enabled = false;
        inContainerGui = false;
        inGuiScreen = false;
        inTooltip = false;
        inItemOverlay = false;
        inChat = false;
        config = null;
        containerFlagLoggedOnce = false;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void clearColorCache() {
        DarkModeFixColorProcessor.clearCache();
    }

    public static void setInContainerGui(boolean value) {
        inContainerGui = value;
        if (value && !containerFlagLoggedOnce) {
            containerFlagLoggedOnce = true;
            GTNHLib.LOG.info(
                    "DarkModeFix inContainerGui=true (inGuiScreen={}, inTooltip={}, inItemOverlay={})",
                    inGuiScreen,
                    inTooltip,
                    inItemOverlay);
        }
    }

    public static void setInGuiScreen(boolean value) {
        inGuiScreen = value;
    }

    public static void setInTooltip(boolean value) {
        inTooltip = value;
    }

    public static void setInItemOverlay(boolean value) {
        inItemOverlay = value;
    }

    public static void setInChat(boolean value) {
        inChat = value;
    }

    private static void log(String message, Object... args) {
        GTNHLib.LOG.debug("[RPDarkModeFix] " + message, args);
    }
}
