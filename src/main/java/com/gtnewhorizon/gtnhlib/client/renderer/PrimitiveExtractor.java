package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.DEFAULT_COLOR;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.VERTEX_SIZE;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.line.ModelLine;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive.ModelPrimitiveView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.tri.ModelTriangle;
import com.gtnewhorizon.gtnhlib.util.ObjectPooler;

/**
 * Utility class for extracting primitives (lines, triangles, quads) from Tessellator raw buffers. Supports GL_QUADS,
 * GL_TRIANGLES, GL_LINES, GL_LINE_STRIP, GL_LINE_LOOP, GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN.
 */
@Deprecated // Replaced in favor of DirectTessellator (see TessellatorManager for more info)
public final class PrimitiveExtractor {

    private static final Logger LOGGER = LogManager.getLogger("PrimitiveExtractor");

    private PrimitiveExtractor() {}

    /**
     * Builds primitives from a Tessellator's raw buffer based on draw mode. Quads are extracted as ModelQuad objects.
     */
    public static void buildPrimitivesFromBuffer(int[] rawBuffer, int vertexCount, int drawMode, boolean hasTexture,
            boolean hasBrightness, boolean hasColor, boolean hasNormals, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelQuad> quadPool, ObjectPooler<ModelTriangle> triPool,
            ObjectPooler<ModelLine> linePool, List<ModelPrimitiveView> collectedPrimitives,
            CapturingTessellator.Flags flags) {
        buildPrimitivesFromBuffer(
                rawBuffer,
                vertexCount,
                drawMode,
                hasTexture,
                hasBrightness,
                hasColor,
                hasNormals,
                offsetX,
                offsetY,
                offsetZ,
                shaderBlockId,
                quadPool,
                triPool,
                linePool,
                collectedPrimitives,
                flags,
                false);
    }

    /**
     * Builds primitives from a Tessellator's raw buffer based on draw mode.
     *
     * @param convertQuadsToTriangles If true, GL_QUADS are converted to GL_TRIANGLES (2 triangles per quad). Useful for
     *                                modern OpenGL compatibility since GL_QUADS is deprecated in 3.1+.
     */
    public static void buildPrimitivesFromBuffer(int[] rawBuffer, int vertexCount, int drawMode, boolean hasTexture,
            boolean hasBrightness, boolean hasColor, boolean hasNormals, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelQuad> quadPool, ObjectPooler<ModelTriangle> triPool,
            ObjectPooler<ModelLine> linePool, List<ModelPrimitiveView> collectedPrimitives,
            CapturingTessellator.Flags flags, boolean convertQuadsToTriangles) {

        flags.copyFrom(hasTexture, hasBrightness, hasColor, hasNormals, drawMode);

        switch (drawMode) {
            case GL11.GL_QUADS:
                if (convertQuadsToTriangles) {
                    extractQuadsAsTriangles(
                            rawBuffer,
                            vertexCount,
                            offsetX,
                            offsetY,
                            offsetZ,
                            shaderBlockId,
                            triPool,
                            collectedPrimitives,
                            flags);
                    flags.drawMode = GL11.GL_TRIANGLES;
                } else {
                    extractQuads(
                            rawBuffer,
                            vertexCount,
                            offsetX,
                            offsetY,
                            offsetZ,
                            shaderBlockId,
                            quadPool,
                            collectedPrimitives,
                            flags);
                }
                break;
            case GL11.GL_TRIANGLES:
                extractTriangles(
                        rawBuffer,
                        vertexCount,
                        offsetX,
                        offsetY,
                        offsetZ,
                        shaderBlockId,
                        triPool,
                        collectedPrimitives,
                        flags);
                break;
            case GL11.GL_LINES:
                extractLines(
                        rawBuffer,
                        vertexCount,
                        offsetX,
                        offsetY,
                        offsetZ,
                        shaderBlockId,
                        linePool,
                        collectedPrimitives);
                break;
            case GL11.GL_LINE_STRIP:
                extractLineStrip(
                        rawBuffer,
                        vertexCount,
                        offsetX,
                        offsetY,
                        offsetZ,
                        shaderBlockId,
                        linePool,
                        collectedPrimitives);
                flags.drawMode = GL11.GL_LINES; // Converted to individual lines
                break;
            case GL11.GL_LINE_LOOP:
                extractLineLoop(
                        rawBuffer,
                        vertexCount,
                        offsetX,
                        offsetY,
                        offsetZ,
                        shaderBlockId,
                        linePool,
                        collectedPrimitives);
                flags.drawMode = GL11.GL_LINES;
                break;
            case GL11.GL_TRIANGLE_STRIP:
                extractTriangleStrip(
                        rawBuffer,
                        vertexCount,
                        offsetX,
                        offsetY,
                        offsetZ,
                        shaderBlockId,
                        triPool,
                        collectedPrimitives,
                        flags);
                flags.drawMode = GL11.GL_TRIANGLES;
                break;
            case GL11.GL_TRIANGLE_FAN:
                extractTriangleFan(
                        rawBuffer,
                        vertexCount,
                        offsetX,
                        offsetY,
                        offsetZ,
                        shaderBlockId,
                        triPool,
                        collectedPrimitives,
                        flags);
                flags.drawMode = GL11.GL_TRIANGLES;
                break;
            default:
                LOGGER.warn(
                        "Unsupported draw mode: 0x{} ({} vertices) - primitives will not be captured",
                        Integer.toHexString(drawMode),
                        vertexCount);
                break;
        }
    }

    // ==================== QUADS ====================

    private static void extractQuads(int[] rawBuffer, int vertexCount, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelQuad> quadPool, List<ModelPrimitiveView> collected,
            CapturingTessellator.Flags flags) {

        final int numQuads = vertexCount / 4;
        for (int i = 0; i < numQuads; i++) {
            final int srcOffset = i * 4 * VERTEX_SIZE;
            if (isEmptyPrimitive(rawBuffer, srcOffset, 4)) continue;

            final ModelQuad quad = quadPool.getInstance();
            quad.setState(rawBuffer, srcOffset, flags, GL11.GL_QUADS, offsetX, offsetY, offsetZ);
            quad.setShaderBlockId(shaderBlockId);
            collected.add(quad);
        }
    }

    /**
     * Converts quads to triangles. Each quad (v0,v1,v2,v3) becomes 2 triangles: (v0,v1,v2) and (v0,v2,v3).
     */
    private static void extractQuadsAsTriangles(int[] rawBuffer, int vertexCount, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelTriangle> triPool, List<ModelPrimitiveView> collected,
            CapturingTessellator.Flags flags) {

        final int numQuads = vertexCount / 4;
        for (int i = 0; i < numQuads; i++) {
            final int srcOffset = i * 4 * VERTEX_SIZE;
            if (isEmptyPrimitive(rawBuffer, srcOffset, 4)) continue;

            // Triangle 1: v0, v1, v2
            final ModelTriangle tri1 = triPool.getInstance();
            copyVertexToTriangle(rawBuffer, srcOffset + 0 * VERTEX_SIZE, tri1, 0);
            copyVertexToTriangle(rawBuffer, srcOffset + 1 * VERTEX_SIZE, tri1, 1);
            copyVertexToTriangle(rawBuffer, srcOffset + 2 * VERTEX_SIZE, tri1, 2);
            if (!flags.hasColor) setDefaultTriangleColors(tri1);
            tri1.setShaderBlockId(shaderBlockId);
            tri1.applyOffset(offsetX, offsetY, offsetZ);
            collected.add(tri1);

            // Triangle 2: v0, v2, v3
            final ModelTriangle tri2 = triPool.getInstance();
            copyVertexToTriangle(rawBuffer, srcOffset + 0 * VERTEX_SIZE, tri2, 0);
            copyVertexToTriangle(rawBuffer, srcOffset + 2 * VERTEX_SIZE, tri2, 1);
            copyVertexToTriangle(rawBuffer, srcOffset + 3 * VERTEX_SIZE, tri2, 2);
            if (!flags.hasColor) setDefaultTriangleColors(tri2);
            tri2.setShaderBlockId(shaderBlockId);
            tri2.applyOffset(offsetX, offsetY, offsetZ);
            collected.add(tri2);
        }
    }

    // ==================== TRIANGLES ====================

    private static void extractTriangles(int[] rawBuffer, int vertexCount, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelTriangle> triPool, List<ModelPrimitiveView> collected,
            CapturingTessellator.Flags flags) {

        final int numTris = vertexCount / 3;
        for (int i = 0; i < numTris; i++) {
            final int srcOffset = i * 3 * VERTEX_SIZE;
            if (isEmptyPrimitive(rawBuffer, srcOffset, 3)) continue;

            final ModelTriangle tri = triPool.getInstance();
            tri.setState(rawBuffer, srcOffset, flags);
            tri.setShaderBlockId(shaderBlockId);
            tri.applyOffset(offsetX, offsetY, offsetZ);
            collected.add(tri);
        }
    }

    private static void extractTriangleStrip(int[] rawBuffer, int vertexCount, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelTriangle> triPool, List<ModelPrimitiveView> collected,
            CapturingTessellator.Flags flags) {

        if (vertexCount < 3) return;

        // Triangle strip: N vertices → N-2 triangles
        // Even triangles: [i, i+1, i+2]
        // Odd triangles: [i+1, i, i+2] (reversed winding)
        for (int i = 0; i < vertexCount - 2; i++) {
            final ModelTriangle tri = triPool.getInstance();

            int v0, v1, v2;
            if ((i & 1) == 0) {
                v0 = i;
                v1 = i + 1;
                v2 = i + 2;
            } else {
                v0 = i + 1;
                v1 = i;
                v2 = i + 2;
            }

            copyVertexToTriangle(rawBuffer, v0 * VERTEX_SIZE, tri, 0);
            copyVertexToTriangle(rawBuffer, v1 * VERTEX_SIZE, tri, 1);
            copyVertexToTriangle(rawBuffer, v2 * VERTEX_SIZE, tri, 2);

            if (!flags.hasColor) setDefaultTriangleColors(tri);

            tri.setShaderBlockId(shaderBlockId);
            tri.applyOffset(offsetX, offsetY, offsetZ);
            collected.add(tri);
        }
    }

    private static void extractTriangleFan(int[] rawBuffer, int vertexCount, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelTriangle> triPool, List<ModelPrimitiveView> collected,
            CapturingTessellator.Flags flags) {

        if (vertexCount < 3) return;

        // Triangle fan: All triangles share vertex 0
        // Triangle i uses vertices [0, i+1, i+2]
        for (int i = 0; i < vertexCount - 2; i++) {
            final ModelTriangle tri = triPool.getInstance();

            copyVertexToTriangle(rawBuffer, 0, tri, 0); // Center vertex
            copyVertexToTriangle(rawBuffer, (i + 1) * VERTEX_SIZE, tri, 1);
            copyVertexToTriangle(rawBuffer, (i + 2) * VERTEX_SIZE, tri, 2);

            if (!flags.hasColor) setDefaultTriangleColors(tri);

            tri.setShaderBlockId(shaderBlockId);
            tri.applyOffset(offsetX, offsetY, offsetZ);
            collected.add(tri);
        }
    }

    // ==================== LINES ====================

    private static void extractLines(int[] rawBuffer, int vertexCount, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelLine> linePool, List<ModelPrimitiveView> collected) {

        final int numLines = vertexCount / 2;
        for (int i = 0; i < numLines; i++) {
            final int srcOffset = i * 2 * VERTEX_SIZE;
            if (isEmptyPrimitive(rawBuffer, srcOffset, 2)) continue;

            final ModelLine line = linePool.getInstance();
            line.setState(rawBuffer, srcOffset);
            line.setShaderBlockId(shaderBlockId);
            line.applyOffset(offsetX, offsetY, offsetZ);
            collected.add(line);
        }
    }

    private static void extractLineStrip(int[] rawBuffer, int vertexCount, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelLine> linePool, List<ModelPrimitiveView> collected) {

        if (vertexCount < 2) return;

        // Line strip: N vertices → N-1 lines
        for (int i = 0; i < vertexCount - 1; i++) {
            final ModelLine line = linePool.getInstance();

            copyVertexToLine(rawBuffer, i * VERTEX_SIZE, line, 0);
            copyVertexToLine(rawBuffer, (i + 1) * VERTEX_SIZE, line, 1);

            line.setShaderBlockId(shaderBlockId);
            line.applyOffset(offsetX, offsetY, offsetZ);
            collected.add(line);
        }
    }

    private static void extractLineLoop(int[] rawBuffer, int vertexCount, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelLine> linePool, List<ModelPrimitiveView> collected) {

        if (vertexCount < 2) return;

        // Line loop: Like strip + closing segment (but don't recurse - apply offsets inline)
        for (int i = 0; i < vertexCount - 1; i++) {
            final ModelLine line = linePool.getInstance();
            copyVertexToLine(rawBuffer, i * VERTEX_SIZE, line, 0);
            copyVertexToLine(rawBuffer, (i + 1) * VERTEX_SIZE, line, 1);
            line.setShaderBlockId(shaderBlockId);
            line.applyOffset(offsetX, offsetY, offsetZ);
            collected.add(line);
        }

        // Add closing segment (last → first)
        final ModelLine closingLine = linePool.getInstance();
        copyVertexToLine(rawBuffer, (vertexCount - 1) * VERTEX_SIZE, closingLine, 0);
        copyVertexToLine(rawBuffer, 0, closingLine, 1);
        closingLine.setShaderBlockId(shaderBlockId);
        closingLine.applyOffset(offsetX, offsetY, offsetZ);
        collected.add(closingLine);
    }

    // ==================== HELPERS ====================

    /**
     * Copies a vertex from raw buffer to triangle. Delegates to ModelTriangle's optimized copy method.
     */
    private static void copyVertexToTriangle(int[] rawBuffer, int srcOffset, ModelTriangle tri, int destIdx) {
        tri.copyVertexFromBuffer(rawBuffer, srcOffset, destIdx);
    }

    /**
     * Copies a vertex from raw buffer to line. Delegates to ModelLine's optimized copy method.
     */
    private static void copyVertexToLine(int[] rawBuffer, int srcOffset, ModelLine line, int destIdx) {
        line.copyVertexFromBuffer(rawBuffer, srcOffset, destIdx);
    }

    private static void setDefaultTriangleColors(ModelTriangle tri) {
        tri.setColor(0, DEFAULT_COLOR);
        tri.setColor(1, DEFAULT_COLOR);
        tri.setColor(2, DEFAULT_COLOR);
    }

    /**
     * Check if a primitive is degenerate (all vertices at the same position). Also returns true if buffer is malformed
     * (insufficient size) to prevent ArrayIndexOutOfBoundsException.
     */
    static boolean isEmptyPrimitive(int[] rawBuffer, int srcOffset, int vertexCount) {
        // Bounds check: need srcOffset + (vertexCount-1)*VERTEX_SIZE + 2 (for Z axis)
        final int requiredSize = srcOffset + (vertexCount - 1) * VERTEX_SIZE + 3; // +3 for X,Y,Z of last vertex
        if (requiredSize > rawBuffer.length) return true; // Treat malformed buffer as empty

        for (int axis = 0; axis < 3; axis++) {
            final int firstVal = rawBuffer[srcOffset + axis];
            for (int v = 1; v < vertexCount; v++) {
                if (firstVal != rawBuffer[srcOffset + v * VERTEX_SIZE + axis]) {
                    return false;
                }
            }
        }
        return true;
    }
}
