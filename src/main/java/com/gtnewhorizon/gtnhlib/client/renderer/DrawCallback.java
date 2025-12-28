package com.gtnewhorizon.gtnhlib.client.renderer;

import java.util.List;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive.ModelPrimitiveView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;

/**
 * Callback interface for per-draw notifications during display list compilation.
 */
public interface DrawCallback {

    /**
     * Called after each Tessellator.draw() during compiling mode. The quads and primitives are pooled objects - caller
     * MUST copy them if needed beyond this call. After this callback returns, they will be released back to the pool.
     * <p>
     * For any single draw call, typically only one list will have content:
     * <ul>
     * <li>GL_QUADS → quads list (as ModelQuad)</li>
     * <li>GL_TRIANGLES → primitives list (as ModelTriangle)</li>
     * <li>GL_LINES, GL_LINE_STRIP, etc. → primitives list (as ModelLine/ModelTriangle)</li>
     * </ul>
     *
     * @param quads      The quads from GL_QUADS draw calls
     * @param primitives The primitives from GL_TRIANGLES, GL_LINES, and other draw modes
     * @param flags      The tessellator flags (hasTexture, hasBrightness, hasColor, hasNormals, drawMode)
     */
    void onDraw(List<ModelQuadViewMutable> quads, List<ModelPrimitiveView> primitives,
            CapturingTessellator.Flags flags);
}
