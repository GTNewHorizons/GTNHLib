package com.gtnewhorizon.gtnhlib.api;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import net.minecraft.client.renderer.Tessellator;

@SuppressWarnings("unused")
public interface CapturingTesselator {
    /**
     * @return True if this thread is capturing quads, false otherwise
     */
    static boolean isCapturing() {
        return TessellatorManager.isCurrentlyCapturing();
    }

    /**
     * @throws IllegalStateException If the thread is not capturing and is not the main one.
     * @return The CapturingTesselator for this thread if capturing, or else {@link Tessellator#instance} if on the main
     * one.
     */
    static Tessellator getThreadTesselator() {
        return TessellatorManager.get();
    }
}
