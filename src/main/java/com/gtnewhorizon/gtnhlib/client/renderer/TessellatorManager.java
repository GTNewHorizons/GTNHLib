package com.gtnewhorizon.gtnhlib.client.renderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

@SuppressWarnings("unused")
public class TessellatorManager {

    private static final Logger LOGGER = LogManager.getLogger("TessellatorManager");

    private enum CaptureMode {
        CAPTURING, // startCapturing() - batch mode until explicit stop
        COMPILING // setCompiling() - per-draw callback mode
    }

    private static class CaptureState {

        final CaptureMode mode;
        final DrawCallback callback; // null for CAPTURING mode, non-null for COMPILING
        final List<CapturedDraw> capturedDraws = new ArrayList<>(); // Stores per-draw quads+flags for nested CAPTURING

        CaptureState(CaptureMode mode, DrawCallback callback) {
            this.mode = mode;
            this.callback = callback;
        }
    }

    private static final ThreadLocal<CapturingTessellator> capturingTessellator = ThreadLocal
            .withInitial(CapturingTessellator::new);

    private static final ThreadLocal<ArrayList<CaptureState>> captureStack = ThreadLocal.withInitial(ArrayList::new);
    private static final Thread mainThread = Thread.currentThread();

    // Recursion protection (compiling is main-thread only)
    private static boolean isInCompilingCallback = false;

    public static Tessellator get() {
        final ArrayList<CaptureState> stack = captureStack.get();
        if (!stack.isEmpty()) {
            return capturingTessellator.get();
        } else if (isOnMainThread()) {
            return Tessellator.instance;
        } else {
            throw new IllegalStateException("Tried to get the Tessellator off the main thread when not capturing!");
        }
    }

    public static boolean isCurrentlyCapturing() {
        CaptureState current = peekState();
        return current != null && current.mode == CaptureMode.CAPTURING;
    }

    /**
     * Checks if the current (top of stack) state is COMPILING mode. Used by CapturingTessellator to determine if draw()
     * should be intercepted for callbacks. Only the current state matters - nested CAPTURING inside COMPILING should
     * capture, not intercept.
     *
     * @return true if the current state is COMPILING
     */
    static boolean isCurrentlyCompiling() {
        CaptureState current = peekState();
        return current != null && current.mode == CaptureMode.COMPILING;
    }

    public static boolean isOnMainThread() {
        return Thread.currentThread() == mainThread;
    }

    public static boolean isMainInstance(Object instance) {
        return instance == Tessellator.instance || isOnMainThread();
    }

    /**
     * Gets the current (top) capture state, or null if stack is empty.
     */
    private static CaptureState peekState() {
        ArrayList<CaptureState> stack = captureStack.get();
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /**
     * Gets the current capture state, throwing if not in expected mode.
     */
    private static CaptureState requireMode(CaptureMode expected, String errorMsg) {
        ArrayList<CaptureState> stack = captureStack.get();
        if (stack.isEmpty() || stack.get(stack.size() - 1).mode != expected) {
            throw new IllegalStateException(errorMsg);
        }
        return stack.get(stack.size() - 1);
    }

    /**
     * Sets the compiling flag on vanilla Tessellator.instance if it implements ITessellatorInstance.
     */
    private static void setVanillaTessellatorCompiling(boolean compiling) {
        if (Tessellator.instance instanceof ITessellatorInstance tessInst) {
            tessInst.gtnhlib$setCompiling(compiling);
        }
    }

    /**
     * Checks if there are any COMPILING states remaining in the stack. Scans from top to bottom (most likely to find
     * recent COMPILING states first).
     */
    private static boolean hasCompilingInStack(ArrayList<CaptureState> stack) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i).mode == CaptureMode.COMPILING) {
                return true;
            }
        }
        return false;
    }

    /**
     * If parent state is CAPTURING and there are uncommitted quads, save them as a draw. This handles nesting: child
     * captures need to preserve parent's pending work.
     */
    private static void saveUncommittedQuadsIfNeeded(ArrayList<CaptureState> stack, CapturingTessellator tess) {
        if (stack.isEmpty() || tess.getQuads().isEmpty()) {
            return;
        }

        CaptureState parent = stack.get(stack.size() - 1);
        if (parent.mode == CaptureMode.CAPTURING) {
            // Deep copy quads before clearing (will be reused by child)
            parent.capturedDraws.add(
                    new CapturedDraw(
                            ModelQuadUtil.deepCopyQuads(tess.getQuads()),
                            new CapturingTessellator.Flags(tess.flags)));
            tess.getQuads().clear();
        }
    }

    public static void startCapturing() {
        startCapturingAndGet();
    }

    public static CapturingTessellator startCapturingAndGet() {
        ArrayList<CaptureState> stack = captureStack.get();
        final CapturingTessellator tess = capturingTessellator.get();

        // Save any uncommitted quads if parent is CAPTURING
        saveUncommittedQuadsIfNeeded(stack, tess);

        // Validate no orphan quads exist
        if (!tess.getQuads().isEmpty()) {
            throw new IllegalStateException("Tried to start capturing with existing collected Quads!");
        }

        tess.storeTranslation();
        stack.add(new CaptureState(CaptureMode.CAPTURING, null));

        return tess;
    }

    /// Stop the CapturingTessellator and return the captured draws with per-draw flags.
    /// The quads are pooled objects - caller MUST copy them if needed beyond their pool lifecycle.
    public static List<CapturedDraw> stopCapturingToDraws() {
        CaptureState currentState = requireMode(CaptureMode.CAPTURING, "Tried to stop capturing when not capturing!");
        ArrayList<CaptureState> stack = captureStack.get();

        final CapturingTessellator tess = capturingTessellator.get();

        // Flush any pending draw
        if (tess.isDrawing) tess.draw();

        // Get the captured draws from the current level (already stored by processDrawForCapturingTessellator)
        stack.remove(stack.size() - 1); // Pop
        final List<CapturedDraw> draws = new ArrayList<>(currentState.capturedDraws);

        tess.restoreTranslation();

        // If nested inside another CAPTURING, we're done - parent's draws are already in parent's capturedDraws
        // Note: tess.getQuads() should be empty since processDrawForCapturingTessellator clears after each draw
        if (!stack.isEmpty() && stack.get(stack.size() - 1).mode == CaptureMode.CAPTURING) {
            // Child is done, parent continues (no restoration needed - parent draws already in parent.capturedDraws)
            tess.getQuads().clear(); // Defensive clear
        } else {
            tess.discard();
        }

        return draws;
    }

    /// Stop the CapturingTessellator and return the pooled quads. The quads are valid until clearQuads() is called on
    /// the CapturingTesselator, which must be done before starting capturing again.
    /// Note: This flattens all draws into a single list, losing per-draw flag information.
    /// Use stopCapturingToDraws() if you need per-draw flags.
    public static List<ModelQuadViewMutable> stopCapturingToPooledQuads() {
        final List<CapturedDraw> draws = stopCapturingToDraws();

        // Flatten all draws into a single quad list
        final List<ModelQuadViewMutable> allQuads = new ArrayList<>();
        for (int i = 0, size = draws.size(); i < size; i++) {
            allQuads.addAll(draws.get(i).quads());
        }

        return allQuads;
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

    /**
     * Populates the passed-in VBO with the data from the CapturingTessellator. If the passed-in VBO is null, it will
     * create & return a new one.
     */
    public static VertexBuffer stopCapturingToVBO(VertexBuffer vbo, VertexFormat format) {
        if (vbo == null) {
            vbo = new VertexBuffer(format, GL11.GL_QUADS);
        }
        return vbo.upload(stopCapturingToBuffer(format));
    }

    /**
     * Same as stopCapturingToVBO, but now wrapping the VBO inside of a VAO for safer & cached attrib pointers. <br>
     * This method is in 99% of cases better since it's both faster and safer. <br>
     * If VAO's are not supported, this will create a VBO instead.
     */
    public static VertexBuffer stopCapturingToVAO(VertexFormat format) {
        return VAOManager.createVAO(format, GL11.GL_QUADS).upload(stopCapturingToBuffer(format));
    }

    /**
     * Populates the passed-in VAO with the data from the CapturingTessellator. If the passed-in VAO is null, it will
     * create & return a new one.
     */
    public static VertexBuffer stopCapturingToVAO(VertexBuffer vao, VertexFormat format) {
        if (vao == null) {
            vao = VAOManager.createVAO(format, GL11.GL_QUADS);
        }
        return vao.upload(stopCapturingToBuffer(format));
    }

    /**
     * Fast check called from ASM-injected bytecode in Tessellator.draw(). Checks if the Tessellator instance is in
     * compiling mode (display list compilation active).
     *
     * @param tess The Tessellator instance
     * @return true if draw() should be intercepted, false otherwise
     */
    public static boolean shouldInterceptDraw(Tessellator tess) {
        return ((ITessellatorInstance) tess).gtnhlib$isCompiling();
    }

    /**
     * Intercepts Tessellator.draw() during display list compilation. Extracts quads from the vanilla Tessellator's
     * buffer and invokes the compiling callback. Only called when shouldInterceptDraw returns true. This is main-thread
     * only (display list compilation happens on the main thread).
     *
     * @param tess The vanilla Tessellator instance
     * @return The result that draw() should return
     */
    public static int interceptDraw(Tessellator tess) {
        // Detect recursive draw() calls from within callback
        if (isInCompilingCallback) {
            throw new IllegalStateException(
                    "Tessellator.draw() called from within a compiling callback - this is not allowed!");
        }

        // Get callback from stack
        CaptureState current = requireMode(CaptureMode.COMPILING, "interceptDraw called but not in COMPILING mode!");
        if (current.callback == null) {
            throw new IllegalStateException("interceptDraw called but callback is null!");
        }

        // Build quads from vanilla tess's buffer using main thread capturing tessellator's infrastructure
        final CapturingTessellator helper = capturingTessellator.get();
        QuadExtractor.buildQuadsFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                true, // Vanilla always has texture
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0, // Vanilla has no offset
                -1, // No shaderBlockId
                helper.quadBuf,
                helper.collectedQuads,
                helper.flags);

        // Invoke callback with collected quads (protect against recursion)
        isInCompilingCallback = true;
        try {
            current.callback.onDraw(helper.collectedQuads, helper.flags);
        } finally {
            isInCompilingCallback = false;
        }
        helper.clearQuads();

        int result = tess.rawBufferIndex * 4;
        ((ITessellatorInstance) tess).discard();
        return result;
    }

    /**
     * Helper for CapturingTessellator to process its draw() call using shared logic.
     */
    static int processDrawForCapturingTessellator(CapturingTessellator tess) {
        CaptureState current = peekState();

        QuadExtractor.buildQuadsFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                -tess.offset.x,
                -tess.offset.y,
                -tess.offset.z,
                tess.shaderBlockId,
                tess.quadBuf,
                tess.collectedQuads,
                tess.flags);

        if (current != null) {
            if (current.mode == CaptureMode.COMPILING) {
                current.callback.onDraw(tess.collectedQuads, tess.flags);
                tess.clearQuads();
            } else if (current.mode == CaptureMode.CAPTURING) {
                // Deep copy quads before pooling (tess.clearQuads releases them for reuse)
                // Store per-draw quads+flags (deep copy to preserve across pool reuse)
                current.capturedDraws.add(
                        new CapturedDraw(
                                ModelQuadUtil.deepCopyQuads(tess.collectedQuads),
                                new CapturingTessellator.Flags(tess.flags)));
                tess.clearQuads();
            }
        }

        int result = tess.rawBufferIndex * 4;
        tess.discard();
        return result;
    }

    public static void cleanup() {
        // Ensure we've cleaned everything up
        final CapturingTessellator tessellator = capturingTessellator.get();
        final ArrayList<CaptureState> stack = captureStack.get();

        // Check if we're cleaning up during active compiling
        if (isOnMainThread()) {
            final CaptureState current = peekState();
            if (current != null && current.mode == CaptureMode.COMPILING) {
                LOGGER.warn(
                        "[TessellatorManager] cleanup() called while compiling is active - this may indicate cleanup() called during display list compilation!",
                        new Exception("Stack trace"));
                setVanillaTessellatorCompiling(false);
            }
        }

        stack.clear();
        tessellator.discard();
        tessellator.clearQuads();
        isInCompilingCallback = false;
    }

    /**
     * Set display list compiling mode with per-draw callbacks. While compiling, each Tessellator.draw() will
     * automatically invoke the callback. The callback receives only the quads from that specific draw call.
     *
     * Nesting: Compiling can be nested inside capturing mode (for VBO capture of display list contents), and COMPILING
     * can now be nested inside COMPILING (for nested glNewList() calls). Each nested level gets its own callback
     * invoked independently.
     *
     * @param callback Called for each draw() with the newly captured quads
     * @throws IllegalArgumentException if callback is null
     */
    public static void setCompiling(DrawCallback callback) {
        if (callback == null) throw new IllegalArgumentException("Callback cannot be null");
        if (!isOnMainThread()) {
            throw new IllegalStateException("Display list compilation can only happen on main thread!");
        }

        ArrayList<CaptureState> stack = captureStack.get();

        // Nested COMPILING is now allowed for nested display list compilation
        // No check needed - just push a new COMPILING state

        final CapturingTessellator tess = capturingTessellator.get();
        if (!tess.getQuads().isEmpty()) {
            throw new IllegalStateException("Tried to start compiling with existing collected Quads!");
        }

        stack.add(new CaptureState(CaptureMode.COMPILING, callback));

        // Set flag on Tessellator.instance if it exists and implements ITessellatorInstance
        // This flag stays true as long as ANY compiling state exists
        setVanillaTessellatorCompiling(true);
        tess.storeTranslation();
    }

    /**
     * Stop display list compiling mode. If there's a pending draw, flushes it and invokes callback one final time.
     * Clears the callback and any remaining quads.
     *
     * For nested COMPILING: Only the innermost (current) compilation is stopped. The parent compilation continues. The
     * compiling flag is only cleared when ALL compilation levels have been exited.
     *
     * @throws IllegalStateException if not currently compiling
     */
    public static void stopCompiling() {
        if (!isOnMainThread()) {
            throw new IllegalStateException("stopCompiling() can only be called from main thread!");
        }

        requireMode(CaptureMode.COMPILING, "Not currently compiling!");
        ArrayList<CaptureState> stack = captureStack.get();

        final CapturingTessellator tess = capturingTessellator.get();

        // Flush any pending draw BEFORE popping stack (draw will still trigger callback via stack check)
        if (tess.isDrawing) {
            tess.draw();
        }

        // Now pop the stack
        stack.remove(stack.size() - 1);

        // Only clear flag if there are no more COMPILING states in the stack
        if (!hasCompilingInStack(stack)) {
            setVanillaTessellatorCompiling(false);
        }

        // Clear any remaining quads (shouldn't be any if callback worked correctly)
        tess.clearQuads();
        tess.discard();
        tess.restoreTranslation();
    }

}
