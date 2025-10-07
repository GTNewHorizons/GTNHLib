package com.gtnewhorizon.gtnhlib.client.renderer.quad;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.client.renderer.quad.properties.ModelQuadFlags;

/**
 * Provides a read-only view of a model quad. For mutable access to a model quad, see {@link ModelQuadViewMutable}.
 */
public interface ModelQuadView {

    /**
     * @return The x-position of the vertex at index {@param idx}
     */
    float getX(int idx);

    /**
     * @return The y-position of the vertex at index {@param idx}
     */
    float getY(int idx);

    /**
     * @return The z-position of the vertex at index {@param idx}
     */
    float getZ(int idx);

    /**
     * @return The integer-encoded color (ABGR?) of the vertex at index {@param idx}
     */
    int getColor(int idx);

    /**
     * @return The texture x-coordinate for the vertex at index {@param idx}
     */
    float getTexU(int idx);

    /**
     * @return The texture y-coordinate for the vertex at index {@param idx}
     */
    float getTexV(int idx);

    /**
     * @return The integer bit flags containing the {@link ModelQuadFlags} for this quad
     */
    int getFlags();

    /**
     * @return The integer-encoded normal vector for the vertex at index {@param idx}
     */
    int getNormal(int idx);

    /**
     * This is the face used for vanilla lighting calculations and will be the block face to which the quad is most
     * closely aligned. Always the same as cull face for quads that are on a block face, but never
     * {@link ForgeDirection#UNKNOWN} or null.
     */
    ForgeDirection getLightFace();

}
