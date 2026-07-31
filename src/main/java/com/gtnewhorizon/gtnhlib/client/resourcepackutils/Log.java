package com.gtnewhorizon.gtnhlib.client.resourcepackutils;

import com.gtnewhorizon.gtnhlib.GTNHLib;

final class Log {

    private Log() {}

    static void debug(String message, Object... args) {
        GTNHLib.LOG.debug("[resourcepackutils] " + message, args);
    }

    static void warn(String message, Object... args) {
        GTNHLib.LOG.warn("[resourcepackutils] " + message, args);
    }
}
