package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

import com.gtnewhorizon.gtnhlib.GTNHLib;

final class RpUpdaterLog {

    private RpUpdaterLog() {}

    static void debug(String message, Object... args) {
        GTNHLib.LOG.debug("[RPUpdater] " + message, args);
    }

    static void warn(String message, Object... args) {
        GTNHLib.LOG.warn("[RPUpdater] " + message, args);
    }
}
