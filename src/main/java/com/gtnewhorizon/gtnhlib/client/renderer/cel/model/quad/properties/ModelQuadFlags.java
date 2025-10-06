package com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.ColorABGR;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.impl.common.util.MathUtil;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.BakedQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

public class ModelQuadFlags {
    /**
     * Indicates that the quad does not fully cover the given face for the model.
     */
    public static final int IS_PARTIAL = 0b001;

    /**
     * Indicates that the quad is parallel to its light face.
     */
    public static final int IS_PARALLEL = 0b010;

    /**
     * Indicates that the quad is aligned to the block grid.
     * This flag is only set if {@link #IS_PARALLEL} is set.
     */
    public static final int IS_ALIGNED = 0b100;

    /**
     * Indicates that the quad should be shaded using vanilla's getShade logic and the light face, rather than
     * the normals of each vertex.
     */
    public static final int IS_VANILLA_SHADED = 0b1000;
    /**
     * Indicates that the particle sprite on this quad can be trusted to be the only sprite it shows.
     */
    public static final int IS_TRUSTED_SPRITE = (1 << 4);
    /**
     * Indicates that this quad can use a more optimal terrain render pass based on its sprite.
     */
    public static final int IS_PASS_OPTIMIZABLE = (1 << 5);
    /**
     * Indicates that the flags are populated for the quad.
     */
    public static final int IS_POPULATED = (1 << 31);

    /**
     * @return True if the bit-flag of {@link ModelQuadFlags} contains the given flag
     */
    public static boolean contains(int flags, int mask) {
        return (flags & mask) != 0;
    }

    public static int getQuadFlags(ModelQuadView quad, ModelQuadFacing face) {
        return getQuadFlags(quad, face, 0);
    }

    /**
     * Checks whether a quad's vertex ordering matches Minecraft's canonical baked order
     * for the given face.
     * <p>
     * Minecraft allows four different permutations of a quad's vertices that will
     * render identically. However, the baked model pipeline (e.g. ambient occlusion,
     * lighting interpolation, and back-face culling) assumes a specific ordering
     * that is hardcoded in the model baking logic. This method verifies
     * that a quad's vertices are listed in that exact canonical order.
     * <p>
     * The canonical order is:
     * <ul>
     *   <li>Vertices are arranged counter-clockwise (CCW) as seen from outside the block face.</li>
     *   <li>Each face (±X, ±Y, ±Z) starts at a specific corner and proceeds CCW around the face.</li>
     *   <li>For example, the -Y face (DOWN) starts at {@code (minX, minY, maxZ)} and proceeds
     *       through {@code (minX, minY, minZ)}, {@code (maxX, minY, minZ)}, {@code (maxX, minY, maxZ)}.</li>
     * </ul>
     * <p>
     * This method avoids allocations by comparing directly against the expected coordinates
     * for each vertex index based on the given {@code face} and bounding box.
     *
     * @param face the face direction of the quad; determines which axis is fixed and
     *             which two axes form the quad plane
     * @return {@code true} if the quad's vertices match Minecraft's canonical baked order
     *         for the given face, {@code false} otherwise
     */
    private static boolean canonicalVertexOrder(ModelQuadView quad, ModelQuadFacing face, float minX, float minY, float minZ,
                                                float maxX, float maxY, float maxZ) {
        return switch (face) {
            case NEG_Y -> MathUtil.roughlyEqual(quad.getX(0), minX) && MathUtil.roughlyEqual(quad.getY(0), minY) && MathUtil.roughlyEqual(quad.getZ(0), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(1), minX) && MathUtil.roughlyEqual(quad.getY(1), minY) && MathUtil.roughlyEqual(quad.getZ(1), minZ)
                    && MathUtil.roughlyEqual(quad.getX(2), maxX) && MathUtil.roughlyEqual(quad.getY(2), minY) && MathUtil.roughlyEqual(quad.getZ(2), minZ)
                    && MathUtil.roughlyEqual(quad.getX(3), maxX) && MathUtil.roughlyEqual(quad.getY(3), minY) && MathUtil.roughlyEqual(quad.getZ(3), maxZ);
            case POS_Y -> MathUtil.roughlyEqual(quad.getX(0), minX) && MathUtil.roughlyEqual(quad.getY(0), maxY) && MathUtil.roughlyEqual(quad.getZ(0), minZ)
                    && MathUtil.roughlyEqual(quad.getX(1), minX) && MathUtil.roughlyEqual(quad.getY(1), maxY) && MathUtil.roughlyEqual(quad.getZ(1), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(2), maxX) && MathUtil.roughlyEqual(quad.getY(2), maxY) && MathUtil.roughlyEqual(quad.getZ(2), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(3), maxX) && MathUtil.roughlyEqual(quad.getY(3), maxY) && MathUtil.roughlyEqual(quad.getZ(3), minZ);
            case NEG_Z -> MathUtil.roughlyEqual(quad.getX(0), maxX) && MathUtil.roughlyEqual(quad.getY(0), maxY) && MathUtil.roughlyEqual(quad.getZ(0), minZ)
                    && MathUtil.roughlyEqual(quad.getX(1), maxX) && MathUtil.roughlyEqual(quad.getY(1), minY) && MathUtil.roughlyEqual(quad.getZ(1), minZ)
                    && MathUtil.roughlyEqual(quad.getX(2), minX) && MathUtil.roughlyEqual(quad.getY(2), minY) && MathUtil.roughlyEqual(quad.getZ(2), minZ)
                    && MathUtil.roughlyEqual(quad.getX(3), minX) && MathUtil.roughlyEqual(quad.getY(3), maxY) && MathUtil.roughlyEqual(quad.getZ(3), minZ);
            case POS_Z -> MathUtil.roughlyEqual(quad.getX(0), minX) && MathUtil.roughlyEqual(quad.getY(0), maxY) && MathUtil.roughlyEqual(quad.getZ(0), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(1), minX) && MathUtil.roughlyEqual(quad.getY(1), minY) && MathUtil.roughlyEqual(quad.getZ(1), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(2), maxX) && MathUtil.roughlyEqual(quad.getY(2), minY) && MathUtil.roughlyEqual(quad.getZ(2), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(3), maxX) && MathUtil.roughlyEqual(quad.getY(3), maxY) && MathUtil.roughlyEqual(quad.getZ(3), maxZ);
            case NEG_X -> MathUtil.roughlyEqual(quad.getX(0), minX) && MathUtil.roughlyEqual(quad.getY(0), maxY) && MathUtil.roughlyEqual(quad.getZ(0), minZ)
                    && MathUtil.roughlyEqual(quad.getX(1), minX) && MathUtil.roughlyEqual(quad.getY(1), minY) && MathUtil.roughlyEqual(quad.getZ(1), minZ)
                    && MathUtil.roughlyEqual(quad.getX(2), minX) && MathUtil.roughlyEqual(quad.getY(2), minY) && MathUtil.roughlyEqual(quad.getZ(2), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(3), minX) && MathUtil.roughlyEqual(quad.getY(3), maxY) && MathUtil.roughlyEqual(quad.getZ(3), maxZ);
            case POS_X -> MathUtil.roughlyEqual(quad.getX(0), maxX) && MathUtil.roughlyEqual(quad.getY(0), maxY) && MathUtil.roughlyEqual(quad.getZ(0), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(1), maxX) && MathUtil.roughlyEqual(quad.getY(1), minY) && MathUtil.roughlyEqual(quad.getZ(1), maxZ)
                    && MathUtil.roughlyEqual(quad.getX(2), maxX) && MathUtil.roughlyEqual(quad.getY(2), minY) && MathUtil.roughlyEqual(quad.getZ(2), minZ)
                    && MathUtil.roughlyEqual(quad.getX(3), maxX) && MathUtil.roughlyEqual(quad.getY(3), maxY) && MathUtil.roughlyEqual(quad.getZ(3), minZ);
            case UNASSIGNED -> false;
        };
    }

    /**
     * Calculates the properties of the given quad. This data is used later by the light pipeline in order to make
     * certain optimizations.
     */
    public static int getQuadFlags(ModelQuadView quad, ModelQuadFacing face, int existingFlags) {
        float minX = 32.0F;
        float minY = 32.0F;
        float minZ = 32.0F;

        float maxX = -32.0F;
        float maxY = -32.0F;
        float maxZ = -32.0F;

        int numVertices = 4;
        if (quad instanceof BakedQuadView bakedQuad) {
            numVertices = Math.min(numVertices, bakedQuad.getVerticesCount());
        }

        boolean degenerate = false, nonOpaqueColor = false;

        for (int i = 0; i < numVertices; ++i) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);

            for (int j = 0; j < i; ++j) {
                float px = quad.getX(j);
                float py = quad.getY(j);
                float pz = quad.getZ(j);

                if (MathUtil.roughlyEqual(px, x) &&
                        MathUtil.roughlyEqual(py, y) &&
                        MathUtil.roughlyEqual(pz, z)) {
                    degenerate = true;
                    break;
                }
            }

            if(ColorABGR.unpackAlpha(quad.getColor(i)) != 255) {
                nonOpaqueColor = true;
            }
        }

        // Do not set the partial flag if the vertices are not listed in the expected order. The optimization assumes
        // it can map directly onto corners in specific locations.
        boolean partial = degenerate || (switch (face.getAxis()) {
            case X -> minY >= 0.0001f || minZ >= 0.0001f || maxY <= 0.9999F || maxZ <= 0.9999F;
            case Y -> minX >= 0.0001f || minZ >= 0.0001f || maxX <= 0.9999F || maxZ <= 0.9999F;
            case Z -> minX >= 0.0001f || minY >= 0.0001f || maxX <= 0.9999F || maxY <= 0.9999F;
        }) || !canonicalVertexOrder(quad, face, minX, minY, minZ, maxX, maxY, maxZ);

        boolean parallel = switch(face.getAxis()) {
            case X -> minX == maxX;
            case Y -> minY == maxY;
            case Z -> minZ == maxZ;
        };

        boolean aligned = parallel && switch (face) {
            case NEG_Y -> minY < 0.0001f;
            case POS_Y -> maxY > 0.9999F;
            case NEG_Z -> minZ < 0.0001f;
            case POS_Z -> maxZ > 0.9999F;
            case NEG_X -> minX < 0.0001f;
            case POS_X -> maxX > 0.9999F;
            case UNASSIGNED -> throw new IllegalArgumentException();
        };

        int flags = existingFlags & ~(IS_PARTIAL | IS_PARALLEL | IS_ALIGNED);

        if (partial) {
            flags |= IS_PARTIAL;
        }

        if (parallel) {
            flags |= IS_PARALLEL;
        }

        if (aligned) {
            flags |= IS_ALIGNED;
        }

        if (!nonOpaqueColor && (flags & IS_TRUSTED_SPRITE) != 0) {
            flags |= IS_PASS_OPTIMIZABLE;
        }

        flags |= IS_POPULATED;

        return flags;
    }
}
