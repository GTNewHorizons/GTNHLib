package com.gtnewhorizon.gtnhlib.client.renderer.tessellator;

import static com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager.DIRECT_TESSELLATOR_STACK_DEPTH;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorCallback;

public final class VertexCallbackManager {

    public static final TessellatorCallback DEFAULT = new TessellatorCallback() {};

    public static TessellatorCallback callback;

    private static final TessellatorCallback[] tessellatorCallbacks;
    private static int callbackIndex = 0; // Points to the TessellatorCallback in the stack

    static {
        tessellatorCallbacks = new TessellatorCallback[DIRECT_TESSELLATOR_STACK_DEPTH];
        tessellatorCallbacks[0] = DEFAULT;
        callback = DEFAULT;
    }

    private static void updateCallback() {
        callback = tessellatorCallbacks[callbackIndex];
    }

    public static void pushCallback(TessellatorCallback callback) {
        if (++callbackIndex >= DIRECT_TESSELLATOR_STACK_DEPTH) {
            throw new IllegalStateException("VertexCallbackManager stack overflow");
        }
        tessellatorCallbacks[callbackIndex] = callback;
        updateCallback();
    }

    public static void popCallback() {
        if (callbackIndex == 0) {
            throw new IllegalStateException("Cannot call popCallback if there are no callbacks!");
        }
        tessellatorCallbacks[callbackIndex--] = null;
        updateCallback();
    }
}
