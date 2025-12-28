package com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive;

/**
 * Base interface for all primitive types (lines, triangles, quads). Provides common vertex accessors with variable
 * vertex count.
 */
public interface ModelPrimitiveView {

    /**
     * @return The number of vertices in this primitive (2 for lines, 3 for triangles, 4 for quads)
     */
    int getVertexCount();

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
     * @return The integer-encoded color of the vertex at index {@param idx}
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
     * @return The packed lightmap coordinates for the vertex at index {@param idx}
     */
    int getLight(int idx);

    /**
     * @return The packed normal vector for the vertex at index {@param idx}
     */
    int getForgeNormal(int idx);
}
