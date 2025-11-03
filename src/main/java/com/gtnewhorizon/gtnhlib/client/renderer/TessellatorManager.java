package com.gtnewhorizon.gtnhlib.client.renderer;

import java.nio.ByteBuffer;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

@SuppressWarnings("unused")
public class TessellatorManager {

    private static final ThreadLocal<CapturingTessellator> capturingTessellator = ThreadLocal
            .withInitial(CapturingTessellator::new);

    private static final ThreadLocal<Boolean> currentlyCapturing = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Thread mainThread = Thread.currentThread();

    public static Tessellator get() {
        if (currentlyCapturing.get()) {
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
        final CapturingTessellator tess = capturingTessellator.get();
        if (tess.getQuads().size() > 0)
            throw new IllegalStateException("Tried to start capturing with existing collected Quads!");
        tess.storeTranslation();

        currentlyCapturing.set(true);
    }

    /*
     * Stop the CapturingTessellator and return the pooled quads. The quads are valid until clearQuads() is called on
     * the CapturingTesselator, which must be done before starting capturing again.
     */
    public static List<QuadView> stopCapturingToPooledQuads() {
        if (!currentlyCapturing.get()) throw new IllegalStateException("Tried to stop capturing when not capturing!");
        currentlyCapturing.set(false);
        final CapturingTessellator tess = capturingTessellator.get();

        // Be sure we got all the quads
        if (tess.isDrawing) tess.draw();

        final List<QuadView> quads = tess.getQuads();
        tess.discard();
        tess.restoreTranslation();

        return quads;
    }

    /*
     * Stops the CapturingTessellator, stores the quads in a buffer (based on the VertexFormat provided), and clears the
     * quads.
     */
    public static ByteBuffer stopCapturingToBuffer(VertexFormat format) {
        final ByteBuffer buf = CapturingTessellator.quadsToBuffer(stopCapturingToPooledQuads(), format);
        capturingTessellator.get().clearQuads();
        return buf;
    }

    /*
     * Stops the CapturingTessellator, stores the quads in a buffer (based on the VertexFormat provided), uploads the
     * buffer to a new VertexBuffer, and clears the quads.
     */
    public static VertexBuffer stopCapturingToVBO(VertexFormat format) {
        return new VertexBuffer(format, GL11.GL_QUADS).upload(stopCapturingToBuffer(format));
    }

    /**
     * Same as stopCapturingToVBO, but now wrapping the VBO inside of a VAO for safer & cached attrib pointers. <br>
     * This method is in 99% of cases better since it's both faster and safer. <br>
     * If VAO's are not supported, this will create a VBO instead.
     */
    public static VertexBuffer stopCapturingToVAO(VertexFormat format) {
        return VAOManager.createVAO(format, GL11.GL_QUADS).upload(stopCapturingToBuffer(format));
    }

    static {
        System.out.println("[TessellatorManager] Initialized on thread " + mainThread.getName());
    }

    public static void cleanup() {
        // Ensure we've cleaned everything up
        final CapturingTessellator tessellator = capturingTessellator.get();

        currentlyCapturing.set(false);
        tessellator.discard();
        tessellator.clearQuads();
    }
}
