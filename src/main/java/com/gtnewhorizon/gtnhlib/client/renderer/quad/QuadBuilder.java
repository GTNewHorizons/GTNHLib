package com.gtnewhorizon.gtnhlib.client.renderer.quad;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

public interface QuadBuilder {

    /**
     * Provides a hint to renderer about the facing of this quad. Not required, but if provided can shortcut some
     * geometric analysis if the quad is parallel to a block face. Should be the expected value of
     * {@link ModelQuadView#getLightFace()}. Value will be confirmed and if invalid the correct light face will be
     * calculated.
     *
     * <p>
     * Null by default, and set automatically by {@link ModelQuadViewMutable#setCullFace(ForgeDirection)}.
     *
     * <p>
     * Models may also find this useful as the face for texture UV locking and rotation semantics.
     *
     * <p>
     * Note: This value is not persisted independently when the quad is encoded. When reading encoded quads, this value
     * will always be the same as {@link ModelQuadView#getLightFace()}.
     */
    void nominalFace(@Nullable ForgeDirection face);

    /**
     * See {@link #nominalFace(ForgeDirection)}
     */
    ForgeDirection nominalFace();

    /**
     * Convenience: access x, y, z by index 0-2.
     */
    float posByIndex(int vertexIndex, int coordinateIndex);

}
