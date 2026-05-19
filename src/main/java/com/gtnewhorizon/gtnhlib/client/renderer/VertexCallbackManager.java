package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager.DIRECT_TESSELLATOR_STACK_DEPTH;

import com.gtnewhorizon.gtnhlib.client.renderer.tessellator.TessellatorCallback;

public final class VertexCallbackManager {

    public static final TessellatorCallback DEFAULT = new TessellatorCallback() {};

    static TessellatorCallback callback;

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

    public static void pushCallback(CallbackTessellator parent, TessellatorCallback callback) {
        if (++callbackIndex >= DIRECT_TESSELLATOR_STACK_DEPTH) {
            callbackIndex--;
            throw new IllegalStateException("VertexCallbackManager stack overflow");
        }
        if (parent != null) {
            parent.needsPopCallback = true;
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
