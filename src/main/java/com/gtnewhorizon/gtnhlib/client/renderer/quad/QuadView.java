package com.gtnewhorizon.gtnhlib.client.renderer.quad;

import net.minecraftforge.common.util.ForgeDirection;

public interface QuadView extends ModelQuadViewMutable {

    boolean isShade();

    boolean isDeleted();

    ForgeDirection getFace();

    void setShaderBlockId(int shaderBlockId);

    int getShaderBlockId();

    /**
     * Present for compatibility with the Tesselator, not recommended for general use.
     */
    void setState(int[] rawBuffer, int offset, Quad.Flags flags, int drawMode, float offsetX, float offsetY,
            float offsetZ);
}
