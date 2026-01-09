package com.gtnewhorizon.gtnhlib.client.renderer;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.Tessellator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.line.ModelLine;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive.ModelPrimitiveView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.tri.ModelTriangle;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

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
        List<ModelQuadViewMutable> savedQuads; // Quads saved from parent when nesting starts

        CaptureState(CaptureMode mode, DrawCallback callback) {
            this.mode = mode;
            this.callback = callback;
        }
    }

    /**
     * Captured geometry as a list of VBOs. Each VBO contains primitives of a single type (lines, triangles, or quads).
     */
    @Desugar
    public record CapturedGeometry(List<VertexBuffer> vbos) {

        public boolean isEmpty() {
            return vbos.isEmpty();
        }

        public void delete() {
            for (int i = 0, size = vbos.size(); i < size; i++) {
                vbos.get(i).delete();
            }
        }

        public void renderAll() {
            for (int i = 0, size = vbos.size(); i < size; i++) {
                vbos.get(i).render();
            }
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
            if (hasDirectTessellator()) return getDirectTessellator();
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
     * If parent state is CAPTURING, save its quads before child starts. The child will use the same collectedQuads
     * list, so we save parent's quads and clear for child use.
     */
    private static void saveParentQuadsIfNeeded(ArrayList<CaptureState> stack, CapturingTessellator tess) {
        if (stack.isEmpty()) {
            return;
        }

        CaptureState parent = stack.get(stack.size() - 1);
        if (parent.mode == CaptureMode.CAPTURING) {
            // Save parent's quads and clear for child
            parent.savedQuads = new ArrayList<>(tess.getQuads());
            tess.getQuads().clear();
        }
    }

    public static void startCapturing() {
        startCapturingAndGet();
    }

    public static CapturingTessellator startCapturingAndGet() {
        ArrayList<CaptureState> stack = captureStack.get();
        final CapturingTessellator tess = capturingTessellator.get();

        // Save parent's quads if parent is CAPTURING
        saveParentQuadsIfNeeded(stack, tess);

        // Validate no orphan quads exist
        if (!tess.getQuads().isEmpty()) {
            throw new IllegalStateException("Tried to start capturing with existing collected Quads!");
        }

        tess.storeTranslation();
        stack.add(new CaptureState(CaptureMode.CAPTURING, null));

        return tess;
    }

    /// Stop the CapturingTessellator and return the pooled quads. The quads are valid until clearQuads() is called on
    /// the CapturingTesselator, which must be done before starting capturing again.
    public static List<ModelQuadViewMutable> stopCapturingToPooledQuads() {
        CaptureState currentState = requireMode(CaptureMode.CAPTURING, "Tried to stop capturing when not capturing!");
        ArrayList<CaptureState> stack = captureStack.get();

        final CapturingTessellator tess = capturingTessellator.get();

        // Flush any pending draw
        if (tess.isDrawing) tess.draw();

        stack.remove(stack.size() - 1); // Pop
        tess.restoreTranslation();

        boolean isNested = !stack.isEmpty() && stack.get(stack.size() - 1).mode == CaptureMode.CAPTURING;

        List<ModelQuadViewMutable> quads;

        if (currentState.savedQuads != null) {
            // Had nested child - combine saved quads with any new quads
            quads = currentState.savedQuads;
            quads.addAll(tess.getQuads());
            tess.getQuads().clear();
        } else if (isNested) {
            // We are a child - return copy of accumulated quads (will clear for parent)
            quads = new ArrayList<>(tess.getQuads());
            tess.getQuads().clear();
        } else {
            // Non-nested common case - return accumulated quads directly
            quads = tess.getQuads();
        }
        // Note: don't discard here - caller owns the pooled quads until clearQuads()

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
        return new VertexBuffer(format).upload(stopCapturingToBuffer(format));
    }

    // --------------- DIRECT TESSELLATOR ---------------

    // Instance to use for capturing to vbo's (package-private)
    // Cannot be used outside the tessellator stack
    static final DirectTessellator mainInstance = new DirectTessellator(Tessellator.byteBuffer);

    private static final int DIRECT_TESSELLATOR_STACK_DEPTH = 16;
    static final DirectTessellator[] directTessellators = new DirectTessellator[DIRECT_TESSELLATOR_STACK_DEPTH];
    static int directTessellatorIndex = -1;

    static DirectTessellator getDirectTessellator() {
        return directTessellators[directTessellatorIndex];
    }

    private static boolean hasDirectTessellator() {
        return directTessellatorIndex != -1;
    }

    public static void stopCapturingDirect() {
        if (!hasDirectTessellator())
            throw new IllegalStateException("Tried to stop capturing when not capturing!");
        final DirectTessellator tessellator = getDirectTessellator();
        directTessellators[directTessellatorIndex--] = null;
        tessellator.onRemovedFromStack();
    }

    public static DirectTessellator startCapturingDirect() {
        if (!hasDirectTessellator()) {
            directTessellators[++directTessellatorIndex] = mainInstance;
            return mainInstance;
        }
        // Will be deleted after being removed from the stack
        ByteBuffer buffer = memAlloc(0x100000);
        DirectTessellator tessellator = new DirectTessellator(buffer, true);
        directTessellators[++directTessellatorIndex] = tessellator;
        return tessellator;
    }

    public static DirectTessellator startCapturingDirect(VertexFormat format) {
        DirectTessellator tessellator = startCapturingDirect();
        tessellator.setVertexFormat(format);
        return tessellator;
    }

    public static DirectTessellator startCapturingDirect(DirectDrawCallback callback) {
        DirectTessellator tessellator = startCapturingDirect();
        tessellator.setDrawCallback(callback);
        return tessellator;
    }

    public static VertexBuffer stopCapturingDirectToVAO() {
        final DirectTessellator tessellator = getDirectTessellator();
        final VertexBuffer vbo = tessellator.uploadToVBO();
        stopCapturingDirect();
        return vbo;
    }

    /**
     * Stops the CapturingTessellator and returns separate VBOs for each primitive type captured. This is the preferred
     * method when capturing mixed geometry (lines, triangles, quads).
     *
     * @param format The vertex format for all VBOs
     * @return CapturedGeometry with separate VBOs (null fields for types not captured)
     */
    @Deprecated // Replaced in favor of DirectTessellator (see TessellatorManager for more info)
    public static CapturedGeometry stopCapturingToGeometry(VertexFormat format) {
        CaptureState currentState = requireMode(CaptureMode.CAPTURING, "Tried to stop capturing when not capturing!");
        ArrayList<CaptureState> stack = captureStack.get();
        final CapturingTessellator tess = capturingTessellator.get();

        // Flush any pending draw
        if (tess.isDrawing) tess.draw();

        stack.remove(stack.size() - 1); // Pop

        tess.restoreTranslation();

        // Group primitives by type using reusable lists from the tessellator
        List<ModelLine> lines = tess.lineListCache;
        List<ModelTriangle> triangles = tess.triangleListCache;
        List<ModelQuadViewMutable> quads = tess.quadListCache;
        lines.clear();
        triangles.clear();
        quads.clear();

        // Use indexed loops to avoid iterator allocation
        List<ModelPrimitiveView> prims = tess.getPrimitives();
        for (int i = 0, size = prims.size(); i < size; i++) {
            ModelPrimitiveView prim = prims.get(i);
            if (prim instanceof ModelLine ml) {
                lines.add(ml);
            } else if (prim instanceof ModelTriangle mt) {
                triangles.add(mt);
            }
            // Note: Quads have been moved to collectedQuads by processDrawForCapturingTessellator
        }

        // Get quads from collectedQuads (where they were moved for backward compat)
        List<ModelQuadViewMutable> collectedQuads = tess.getQuads();
        for (int i = 0, size = collectedQuads.size(); i < size; i++) {
            ModelQuadViewMutable quad = collectedQuads.get(i);
            quads.add(quad);
        }

        // Create VBOs for each type that has content
        List<VertexBuffer> vbos = new ArrayList<>();
        if (!lines.isEmpty()) vbos.add(createLineVBO(lines, format));
        if (!triangles.isEmpty()) vbos.add(createTriangleVBO(triangles, format));
        if (!quads.isEmpty()) vbos.add(createQuadVBO(quads, format));

        // Clean up
        tess.clearPrimitives();
        tess.clearQuads();

        if (!stack.isEmpty() && stack.get(stack.size() - 1).mode == CaptureMode.CAPTURING) {
            // Nested - parent continues
        } else {
            tess.discard();
        }

        return new CapturedGeometry(vbos);
    }

    @Deprecated
    private static VertexBuffer createLineVBO(List<ModelLine> lines, VertexFormat format) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(format.getVertexSize() * lines.size() * 2);
        for (int i = 0, size = lines.size(); i < size; i++) {
            writePrimitiveToBuffer(lines.get(i), buffer, format);
        }
        buffer.flip();
        return new VertexBuffer(format, GL11.GL_LINES).upload(buffer);
    }

    @Deprecated
    private static VertexBuffer createTriangleVBO(List<ModelTriangle> triangles, VertexFormat format) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(format.getVertexSize() * triangles.size() * 3);
        for (int i = 0, size = triangles.size(); i < size; i++) {
            writePrimitiveToBuffer(triangles.get(i), buffer, format);
        }
        buffer.flip();
        return new VertexBuffer(format, GL11.GL_TRIANGLES).upload(buffer);
    }

    @Deprecated
    private static VertexBuffer createQuadVBO(List<ModelQuadViewMutable> quads, VertexFormat format) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(format.getVertexSize() * quads.size() * 4);
        format.writeQuads(quads, buffer);
        buffer.flip();
        return new VertexBuffer(format).upload(buffer);
    }

    /**
     * Expected vertex size for primitive writing: pos(12) + color(4) + tex(8) + light(4) + normal(4) = 32 bytes
     */
    private static final int EXPECTED_PRIMITIVE_VERTEX_SIZE = 32;

    /**
     * Writes a primitive's vertices to the buffer using the standard vertex layout.
     * <p>
     * <b>IMPORTANT:</b> This method assumes the vertex format matches the layout:
     * <ul>
     * <li>Position: 3 floats (12 bytes)</li>
     * <li>Color: 1 int ABGR (4 bytes)</li>
     * <li>Texture: 2 floats (8 bytes)</li>
     * <li>Light: 1 int (4 bytes)</li>
     * <li>Normal: 1 int (4 bytes)</li>
     * </ul>
     * Total: 32 bytes per vertex. The format's vertexSize must match.
     *
     * @throws IllegalArgumentException if format.getVertexSize() != 32
     */
    private static void writePrimitiveToBuffer(ModelPrimitiveView prim, ByteBuffer buffer, VertexFormat format) {
        if (format.getVertexSize() != EXPECTED_PRIMITIVE_VERTEX_SIZE) {
            throw new IllegalArgumentException(
                    "writePrimitiveToBuffer requires vertex size of " + EXPECTED_PRIMITIVE_VERTEX_SIZE
                            + " bytes, but format has "
                            + format.getVertexSize()
                            + ". Use a compatible format or implement a custom primitive writer.");
        }

        // Write each vertex of the primitive
        for (int i = 0; i < prim.getVertexCount(); i++) {
            // Position (always present)
            buffer.putFloat(prim.getX(i));
            buffer.putFloat(prim.getY(i));
            buffer.putFloat(prim.getZ(i));

            // Color (ABGR format for OpenGL)
            buffer.putInt(prim.getColor(i));

            // Texture coordinates
            buffer.putFloat(prim.getTexU(i));
            buffer.putFloat(prim.getTexV(i));

            // Light (packed lightmap)
            buffer.putInt(prim.getLight(i));

            // Normal (packed)
            buffer.putInt(prim.getForgeNormal(i));
        }
    }

    /**
     * Populates the passed-in VBO with the data from the CapturingTessellator. If the passed-in VBO is null, it will
     * create & return a new one.
     */
    public static VertexBuffer stopCapturingToVBO(VertexBuffer vbo, VertexFormat format) {
        if (vbo == null) {
            vbo = new VertexBuffer(format);
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
        return ((ITessellatorInstance) tess).gtnhlib$isCompiling()
                || (hasDirectTessellator() && !isCurrentlyCapturing()); // Capturing has priority over Direct
    }

    /**
     * Intercepts Tessellator.draw() during display list compilation. Extracts geometry from the vanilla Tessellator's
     * buffer and invokes the compiling callback. Only called when shouldInterceptDraw returns true. This is main-thread
     * only (display list compilation happens on the main thread).
     * <p>
     * GL_QUADS go to collectedQuads; GL_TRIANGLES and other modes go to collectedPrimitives as proper primitives.
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

        if (hasDirectTessellator()) {
            final DirectTessellator tessellator = getDirectTessellator();
            int result = tessellator.interceptDraw(tess);

            ((ITessellatorInstance) tess).discard();
            return result;
        }

        // Get callback from stack
        CaptureState current = requireMode(CaptureMode.COMPILING, "interceptDraw called but not in COMPILING mode!");
        if (current.callback == null) {
            throw new IllegalStateException("interceptDraw called but callback is null!");
        }

        // Build geometry from vanilla tess's buffer using main thread capturing tessellator's infrastructure
        // COMPILING mode: Only GL_QUADS go to quads; triangles and other primitives stay as proper primitives
        final CapturingTessellator helper = capturingTessellator.get();
        if (tess.drawMode == GL11.GL_QUADS) {
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
                    helper.quadPool,
                    helper.collectedQuads,
                    helper.flags);
        } else {
            // GL_TRIANGLES, GL_LINES, GL_LINE_STRIP, etc. → primitives (not quadrangulated)
            PrimitiveExtractor.buildPrimitivesFromBuffer(
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
                    helper.quadPool,
                    helper.triPool,
                    helper.linePool,
                    helper.collectedPrimitives,
                    helper.flags);
        }

        // Invoke callback with collected geometry (protect against recursion)
        isInCompilingCallback = true;
        try {
            current.callback.onDraw(helper.collectedQuads, helper.collectedPrimitives, helper.flags);
        } finally {
            isInCompilingCallback = false;
        }
        helper.clearQuads();
        helper.clearPrimitives();

        int result = tess.rawBufferIndex * 4;
        ((ITessellatorInstance) tess).discard();
        return result;
    }

    /**
     * Helper for CapturingTessellator to process its draw() call using shared logic.
     * <p>
     * Mode-specific behavior:
     * <ul>
     * <li>COMPILING: Only GL_QUADS → quads; GL_TRIANGLES and other modes → primitives (proper triangles)</li>
     * <li>CAPTURING: GL_QUADS and GL_TRIANGLES → quads (quadrangulated for backward compat); other modes →
     * primitives</li>
     * </ul>
     */
    static int processDrawForCapturingTessellator(CapturingTessellator tess) {
        final CaptureState current = peekState();
        final boolean isCompiling = current != null && current.mode == CaptureMode.COMPILING;

        if (isCompiling) {
            // COMPILING: Only GL_QUADS to quads; triangles and other primitives stay as proper primitives
            if (tess.drawMode == GL11.GL_QUADS) {
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
                        tess.quadPool,
                        tess.collectedQuads,
                        tess.flags);
            } else {
                // GL_TRIANGLES, GL_LINES, GL_LINE_STRIP, etc. → primitives (not quadrangulated)
                PrimitiveExtractor.buildPrimitivesFromBuffer(
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
                        tess.quadPool,
                        tess.triPool,
                        tess.linePool,
                        tess.collectedPrimitives,
                        tess.flags);
            }
            // Invoke callback and clear
            current.callback.onDraw(tess.collectedQuads, tess.collectedPrimitives, tess.flags);
            tess.clearQuads();
            tess.clearPrimitives();
        } else {
            // CAPTURING: GL_QUADS and GL_TRIANGLES to quads (backward compat quadrangulation)
            if (tess.drawMode == GL11.GL_QUADS || tess.drawMode == GL11.GL_TRIANGLES) {
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
                        tess.quadPool,
                        tess.collectedQuads,
                        tess.flags);
            } else {
                // GL_LINES, GL_LINE_STRIP, etc. → primitives
                PrimitiveExtractor.buildPrimitivesFromBuffer(
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
                        tess.quadPool,
                        tess.triPool,
                        tess.linePool,
                        tess.collectedPrimitives,
                        tess.flags);
            }
            // No callback - geometry stays until stop is called
        }

        final int result = tess.rawBufferIndex * 4;
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
        tessellator.clearPrimitives();
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
    @Deprecated // Replaced in favor of DirectTessellator (see TessellatorManager for more info)
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
    @Deprecated // Replaced in favor of DirectTessellator (see TessellatorManager for more info)
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
