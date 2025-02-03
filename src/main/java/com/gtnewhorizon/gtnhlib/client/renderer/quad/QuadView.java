package com.gtnewhorizon.gtnhlib.client.renderer.quad;

import net.minecraftforge.common.util.ForgeDirection;

public interface QuadView extends ModelQuadViewMutable {

    /**
     * Allocates a new quad - use sparingly, and not at all in render paths if you can help it. We don't need another
     * Malice Doors.
     */
    static QuadView allocate() {
        return new Quad();
    }

    boolean isShade();

    boolean isDeleted();

    ForgeDirection getFace();

    QuadView copyFrom(QuadView src);

    int[] getRawData();

    void setShaderBlockId(int shaderBlockId);

    int getShaderBlockId();

    /**
     * Present for compatibility with the Tesselator, not recommended for general use.
     */
    void setState(int[] rawBuffer, int offset, Quad.Flags flags, int drawMode, float offsetX, float offsetY,
            float offsetZ);
}
