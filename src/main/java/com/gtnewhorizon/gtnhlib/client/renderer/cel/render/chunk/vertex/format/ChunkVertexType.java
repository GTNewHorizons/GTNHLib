package com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.vertex.format;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.gl.attribute.GlVertexFormat;

public interface ChunkVertexType {

    /**
     * @return The scale to be applied to vertex coordinates
     */
    float getPositionScale();

    /**
     * @return The translation to be applied to vertex coordinates
     */
    float getPositionOffset();

    /**
     * @return The scale to be applied to texture coordinates
     */
    float getTextureScale();

    GlVertexFormat getVertexFormat();

    /**
     * {@return a newly constructed instance of a vertex encoder for the given vertex type}
     */
    ChunkVertexEncoder createEncoder();

    @MustBeInvokedByOverriders
    default Map<String, String> getDefines() {
        var defines = new HashMap<String, String>();
        defines.put("VERT_POS_SCALE", String.valueOf(this.getPositionScale()));
        defines.put("VERT_POS_OFFSET", String.valueOf(this.getPositionOffset()));
        defines.put("VERT_TEX_SCALE", String.valueOf(this.getTextureScale()));

        return defines;
    }
}
