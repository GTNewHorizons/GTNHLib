package com.gtnewhorizon.gtnhlib.compat;

import com.falsepattern.falsetweaks.api.ThreadedChunkUpdates;

public class FalseTweaks {

    /**
     * When FalseTweaks is loaded, it may inject into the Tesselator to do its own threaded chunk building. If it's
     * doing that, disable our checks and let FT handle it.
     */
    public static boolean doTessSafetyChecks() {
        return !ThreadedChunkUpdates.isEnabled();
    }
}
