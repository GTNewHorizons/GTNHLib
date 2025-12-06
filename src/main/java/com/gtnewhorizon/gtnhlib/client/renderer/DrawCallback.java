package com.gtnewhorizon.gtnhlib.client.renderer;

import java.util.List;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;

/**
 * Callback interface for per-draw notifications during display list compilation.
 */
public interface DrawCallback {

    /**
     * Called after each Tessellator.draw() during compiling mode. The quads are pooled objects - caller MUST copy them
     * if needed beyond this call. After this callback returns, the quads will be released back to the pool.
     *
     * @param quads The quads from this specific draw call (sublist of collected quads)
     * @param flags The tessellator flags (hasTexture, hasBrightness, hasColor, hasNormals) from this draw call
     */
    void onDraw(List<ModelQuadViewMutable> quads, CapturingTessellator.Flags flags);
}
