package com.gtnewhorizon.gtnhlib.api;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;

public class CapturingTesselator {

    public static boolean isCapturing() {
        return TessellatorManager.isCurrentlyCapturing();
    }
}
