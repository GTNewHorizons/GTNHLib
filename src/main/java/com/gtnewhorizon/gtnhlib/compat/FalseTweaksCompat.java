package com.gtnewhorizon.gtnhlib.compat;

import com.falsepattern.falsetweaks.api.Modules;

public class FalseTweaksCompat {

    private static final boolean PRESENT;

    static {
        boolean present = false;
        try {
            Modules.class.getName();
            present = true;
        } catch (Throwable ignored) {}
        PRESENT = present;
    }

    public static boolean threadingActive() {
        if (PRESENT) {
            return ThreadingCompat.threadingActive();
        } else {
            return false;
        }
    }

    private static class ThreadingCompat {

        public static boolean threadingActive() {
            return Modules.threadingActive();
        }
    }
}
