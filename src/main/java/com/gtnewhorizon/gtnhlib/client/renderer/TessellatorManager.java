package com.gtnewhorizon.gtnhlib.client.renderer;

import java.nio.ByteBuffer;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

@SuppressWarnings("unused")
public class TessellatorManager {

    /**
     * Callback interface for per-draw notifications during display list compilation.
     */
    public interface DrawCallback {

        /**
         * Called after each Tessellator.draw() during compiling mode. The quads are pooled objects - caller MUST copy
         * them if needed beyond this call. After this callback returns, the quads will be released back to the pool.
         *
         * @param quads The quads from this specific draw call (sublist of collected quads)
         */
        void onDraw(List<ModelQuadViewMutable> quads);
    }

    private static final ThreadLocal<CapturingTessellator> capturingTessellator = ThreadLocal
            .withInitial(CapturingTessellator::new);

    private static final ThreadLocal<Boolean> currentlyCapturing = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<DrawCallback> compilingCallback = ThreadLocal.withInitial(() -> null);
    private static final Thread mainThread = Thread.currentThread();

    public static Tessellator get() {
        if (currentlyCapturing.get() || compilingCallback.get() != null) {
            return capturingTessellator.get();
        } else if (isOnMainThread()) {
            return Tessellator.instance;
        } else {
            // TODO: Verify this works correctly and nothing unexpected is grabbing a tessellator off the main thread
            // when not capturing
            throw new IllegalStateException("Tried to get the Tessellator off the main thread when not capturing!");
        }
    }

    public static boolean isCurrentlyCapturing() {
        return currentlyCapturing.get();
    }

    public static boolean isOnMainThread() {
        return Thread.currentThread() == mainThread;
    }

    public static boolean isMainInstance(Object instance) {
        return instance == Tessellator.instance || isOnMainThread();
    }

    public static void startCapturing() {
        if (currentlyCapturing.get())
            throw new IllegalStateException("Tried to start capturing when already capturing!");
        if (compilingCallback.get() != null)
            throw new IllegalStateException("Tried to start capturing when already compiling!");
        final CapturingTessellator tess = capturingTessellator.get();
        if (!tess.getQuads().isEmpty())
            throw new IllegalStateException("Tried to start capturing with existing collected Quads!");
        tess.storeTranslation();

        currentlyCapturing.set(true);
    }

    /// Stop the CapturingTessellator and return the pooled quads. The quads are valid until clearQuads() is called on
    /// the CapturingTesselator, which must be done before starting capturing again.
    public static List<ModelQuadViewMutable> stopCapturingToPooledQuads() {
        if (!currentlyCapturing.get()) throw new IllegalStateException("Tried to stop capturing when not capturing!");
        currentlyCapturing.set(false);
        final CapturingTessellator tess = capturingTessellator.get();

        // Be sure we got all the quads
        if (tess.isDrawing) tess.draw();

        final var quads = tess.getQuads();
        tess.discard();
        tess.restoreTranslation();

        return quads;
    }

    /// Stops the CapturingTessellator, stores the quads in a buffer (based on the VertexFormat provided), and clears
    /// the quads.
    public static ByteBuffer stopCapturingToBuffer(VertexFormat format) {
        final ByteBuffer buf = CapturingTessellator.quadsToBuffer(stopCapturingToPooledQuads(), format);
        capturingTessellator.get().clearQuads();
        return buf;
    }

    /// Stops the CapturingTessellator, stores the quads in a buffer (based on the VertexFormat provided), uploads the
    /// buffer to a new VertexBuffer, and clears the quads.
    public static VertexBuffer stopCapturingToVBO(VertexFormat format) {
        return new VertexBuffer(format, GL11.GL_QUADS).upload(stopCapturingToBuffer(format));
    }

    static {
        System.out.println("[TessellatorManager] Initialized on thread " + mainThread.getName());
    }

    public static void cleanup() {
        // Ensure we've cleaned everything up
        final CapturingTessellator tessellator = capturingTessellator.get();

        currentlyCapturing.set(false);
        compilingCallback.set(null);
        tessellator.discard();
        tessellator.clearQuads();
    }

    /**
     * Set display list compiling mode with per-draw callbacks. While compiling, each Tessellator.draw() will
     * automatically invoke the callback. The callback receives only the quads from that specific draw call.
     *
     * @param callback Called for each draw() with the newly captured quads
     * @throws IllegalArgumentException if callback is null
     * @throws IllegalStateException    if already compiling or already capturing
     */
    public static void setCompiling(DrawCallback callback) {
        if (callback == null) throw new IllegalArgumentException("Callback cannot be null");
        if (compilingCallback.get() != null) throw new IllegalStateException("Already compiling!");
        if (currentlyCapturing.get())
            throw new IllegalStateException("Tried to start compiling when already capturing!");
        final CapturingTessellator tess = capturingTessellator.get();
        if (!tess.getQuads().isEmpty())
            throw new IllegalStateException("Tried to start compiling with existing collected Quads!");
        tess.storeTranslation();

        compilingCallback.set(callback);
    }

    /**
     * Stop display list compiling mode. If there's a pending draw, flushes it and invokes callback one final time.
     * Clears the callback and any remaining quads.
     *
     * @throws IllegalStateException if not currently compiling
     */
    public static void stopCompiling() {
        if (compilingCallback.get() == null) throw new IllegalStateException("Not currently compiling!");

        final CapturingTessellator tess = capturingTessellator.get();

        // If there's a pending draw, flush it (this will trigger callback one last time)
        if (tess.isDrawing) {
            tess.draw();
        }

        // Clear any remaining quads (shouldn't be any if callback worked correctly)
        tess.clearQuads();
        tess.discard();
        tess.restoreTranslation();

        compilingCallback.set(null);
    }

    /**
     * Package-private accessor for CapturingTessellator to get the current compiling callback.
     *
     * @return The current callback, or null if not compiling
     */
    static DrawCallback getCompilingCallback() {
        return compilingCallback.get();
    }
}
