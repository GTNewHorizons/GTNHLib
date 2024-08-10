package com.gtnewhorizon.gtnhlib.client.renderer;

public class FaceBehaviorManager {

    // ThreadLocal instance to hold the behavior flag for each thread
    private static final ThreadLocal<Boolean> captureVertexNormals = ThreadLocal.withInitial(() -> false);

    // Method to set the behavior flag
    public static void setVertexNormalBehavior(boolean flag) {
        captureVertexNormals.set(flag);
    }

    // Method to get the behavior flag
    public static boolean getVertexNormalBehavior() {
        return captureVertexNormals.get();
    }

    // Method to clear the behavior flag
    public static void clearVertexNormalBehavior() {
        captureVertexNormals.remove();
    }
}
