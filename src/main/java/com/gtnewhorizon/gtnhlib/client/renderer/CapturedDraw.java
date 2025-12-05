package com.gtnewhorizon.gtnhlib.client.renderer;

import java.util.List;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;

/**
 * Represents a single draw call captured during CAPTURING mode. Contains the quads and flags from that specific draw()
 * call.
 */
public class CapturedDraw {

    private final List<ModelQuadViewMutable> quads;
    private final CapturingTessellator.Flags flags;

    public CapturedDraw(List<ModelQuadViewMutable> quads, CapturingTessellator.Flags flags) {
        this.quads = quads;
        this.flags = flags;
    }

    public List<ModelQuadViewMutable> quads() {
        return quads;
    }

    public CapturingTessellator.Flags flags() {
        return flags;
    }
}
