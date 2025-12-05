package com.gtnewhorizon.gtnhlib.client.renderer;

import java.util.List;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.util.ObjectPooler;

/**
 * Utility class for extracting ModelQuad objects from Tessellator raw buffers. This is used by both vanilla Tessellator
 * (during display list compilation) and CapturingTessellator.
 */
public final class QuadExtractor {

    private static final int VERTEX_STRIDE = 8; // ints per vertex in Tessellator raw buffer

    private QuadExtractor() {
        // Utility class - no instantiation
    }

    /**
     * Builds ModelQuad objects from a Tessellator's raw buffer. Used by both vanilla Tessellator (during compiling) and
     * CapturingTessellator.
     *
     * @param rawBuffer      The Tessellator's raw int[] buffer
     * @param vertexCount    Number of vertices in the buffer
     * @param drawMode       GL draw mode (GL_QUADS, GL_TRIANGLES, etc.)
     * @param hasTexture     Whether texture coordinates are present
     * @param hasBrightness  Whether brightness/light data is present
     * @param hasColor       Whether color data is present
     * @param hasNormals     Whether normal vectors are present
     * @param offsetX        X offset to apply to all vertices
     * @param offsetY        Y offset to apply to all vertices
     * @param offsetZ        Z offset to apply to all vertices
     * @param shaderBlockId  Shader block ID to assign to quads
     * @param quadBuf        Object pooler for ModelQuad instances
     * @param collectedQuads List to add extracted quads to
     * @param flags          Flags object to update with vertex format information
     */
    public static void buildQuadsFromBuffer(int[] rawBuffer, int vertexCount, int drawMode, boolean hasTexture,
            boolean hasBrightness, boolean hasColor, boolean hasNormals, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelQuad> quadBuf, List<ModelQuadViewMutable> collectedQuads,
            CapturingTessellator.Flags flags) {

        flags.copyFrom(hasTexture, hasBrightness, hasColor, hasNormals);

        final int verticesPerPrimitive = drawMode == GL11.GL_QUADS ? 4 : 3;

        for (int quadI = 0; quadI < vertexCount / verticesPerPrimitive; quadI++) {
            final int srcOffset = quadI * (verticesPerPrimitive * VERTEX_STRIDE);
            if (isEmptyQuad(rawBuffer, srcOffset)) continue;

            final ModelQuad quad = quadBuf.getInstance();
            quad.setState(rawBuffer, srcOffset, flags, drawMode, offsetX, offsetY, offsetZ);
            quad.setShaderBlockId(shaderBlockId);
            collectedQuads.add(quad);
        }
    }

    /**
     * Check if a quad is degenerate (all vertices at the same position). This filters out empty/collapsed quads that
     * would be invisible. Compares raw float bits for exact equality.
     *
     * @param rawBuffer The tessellator's raw data buffer
     * @param srcOffset Offset to the first vertex of the quad
     * @return true if all 4 vertices have identical X, Y, and Z coordinates
     */
    private static boolean isEmptyQuad(int[] rawBuffer, int srcOffset) {
        // Check if all 4 vertices have identical X, Y, Z coordinates
        for (int axis = 0; axis < 3; axis++) {
            final int firstVal = rawBuffer[srcOffset + axis];
            if (firstVal != rawBuffer[srcOffset + VERTEX_STRIDE + axis]) return false;
            if (firstVal != rawBuffer[srcOffset + 2 * VERTEX_STRIDE + axis]) return false;
            if (firstVal != rawBuffer[srcOffset + 3 * VERTEX_STRIDE + axis]) return false;
        }
        return true;
    }
}
