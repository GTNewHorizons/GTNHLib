package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.VERTEX_SIZE;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.util.ObjectPooler;

/**
 * Utility class for extracting ModelQuad objects from Tessellator raw buffers. This is used by both vanilla Tessellator
 * (during display list compilation) and CapturingTessellator.
 */
public final class QuadExtractor {

    private static final Logger LOGGER = LogManager.getLogger("QuadExtractor");

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
     * @param quadPool       Object pooler for ModelQuad instances
     * @param collectedQuads List to add extracted quads to
     * @param flags          Flags object to update with vertex format information
     */
    public static void buildQuadsFromBuffer(int[] rawBuffer, int vertexCount, int drawMode, boolean hasTexture,
            boolean hasBrightness, boolean hasColor, boolean hasNormals, int offsetX, int offsetY, int offsetZ,
            int shaderBlockId, ObjectPooler<ModelQuad> quadPool, List<ModelQuadViewMutable> collectedQuads,
            CapturingTessellator.Flags flags) {

        flags.copyFrom(hasTexture, hasBrightness, hasColor, hasNormals, drawMode);

        // Only handle GL_QUADS and GL_TRIANGLES - skip unsupported draw modes to prevent buffer corruption
        final int verticesPerPrimitive;
        if (drawMode == GL11.GL_QUADS) {
            verticesPerPrimitive = 4;
        } else if (drawMode == GL11.GL_TRIANGLES) {
            verticesPerPrimitive = 3;
        } else {
            // Unsupported draw mode (GL_LINES, GL_LINE_STRIP, etc.) - skip to avoid corrupting buffer reads
            LOGGER.debug(
                    "Skipping unsupported draw mode: 0x{} ({} vertices)",
                    Integer.toHexString(drawMode),
                    vertexCount);
            return;
        }

        for (int quadI = 0; quadI < vertexCount / verticesPerPrimitive; quadI++) {
            final int srcOffset = quadI * (verticesPerPrimitive * VERTEX_SIZE);
            if (PrimitiveExtractor.isEmptyPrimitive(rawBuffer, srcOffset, verticesPerPrimitive)) continue;

            final ModelQuad quad = quadPool.getInstance();
            quad.setState(rawBuffer, srcOffset, flags, drawMode, offsetX, offsetY, offsetZ);
            quad.setShaderBlockId(shaderBlockId);
            collectedQuads.add(quad);
        }
    }
}
