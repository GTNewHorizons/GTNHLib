package com.gtnewhorizon.gtnhlib.client.renderer.cel.util;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Z;
import static java.lang.Math.abs;
import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

/// Provides some utilities and constants for interacting with vanilla's model quad vertex format.
/// This is the vertex format used by Minecraft 7.10 for chunk meshes and model quads. Internally, it uses integer
/// arrays for store baked quad data, and as such the following table provides both the byte and int indices.
///
/// | Byte Index | Integer Index | Name | Format | Fields |
/// | ---------- | ------------- | ---- | ------ | ------ |
/// | 0..11 | 0..2 | Position | 3 floats | x, y, z |
/// | 12..19 | 3..4 | Block Texture | 2 floats | u, v |
/// | 19..23 | 5 | Color | 4 unsigned bytes | a, b, g, r \[1] |
/// | 24..27 | 6 | Normal | 3 unsigned bytes | x, y, z |
/// | 28..31 | 7 | Light Texture | 2 shorts | u, v |
///
/// \[1]: ABGR on little-endian systems, RGBA on big-endian systems.
public class ModelQuadUtil {

    // Integer indices for vertex attributes, useful for accessing baked quad data
    public static final int POSITION_INDEX = 0, COLOR_INDEX = 5, TEXTURE_INDEX = 3, LIGHT_INDEX = 7, NORMAL_INDEX = 6;

    // Size of vertex format in 4-byte integers
    public static final int VERTEX_SIZE = 8;

    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    public static final int DEFAULT_LIGHTMAP = 15 << 20 | 15 << 4;

    /// @param vertexIndex The index of the vertex to access
    /// @return The starting offset of the vertex's attributes
    public static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }

    public static ModelQuadFacing findNormalFace(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return ModelQuadFacing.UNASSIGNED;
        }

        float maxDot = 0;
        ModelQuadFacing closestFace = null;

        for (ModelQuadFacing face : ModelQuadFacing.DIRECTIONS) {
            float dot = (x * face.getStepX()) + (y * face.getStepY()) + (z * face.getStepZ());

            if (dot > maxDot) {
                maxDot = dot;
                closestFace = face;
            }
        }

        if (closestFace != null && abs(maxDot - 1.0f) < 1.0E-5F) {
            return closestFace;
        }

        return ModelQuadFacing.UNASSIGNED;
    }

    public static ModelQuadFacing findNormalFace(int normal) {
        return findNormalFace(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
    }

    public static ModelQuadFacing findLightFace(int normal) {
        float x = NormI8.unpackX(normal);
        float y = NormI8.unpackY(normal);
        float z = NormI8.unpackZ(normal);
        var max = (max(max((abs(x)), abs(y)), abs(z)));

        if (max == x) return x < 0 ? NEG_X : POS_X;
        if (max == z) return z < 0 ? NEG_Z : POS_Z;
        return y < 0 ? NEG_Y : POS_Y;
    }

    public static int calculateNormal(ModelQuadView quad) {
        // Now using Newell's method for computing polygon normals.
        // This handles both proper quads and degenerate quads (triangles) correctly. For degenerate quads where v3=v2,
        // the edge v2→v3 contributes zero, naturally giving the correct triangle normal.
        final float x0 = quad.getX(0);
        final float y0 = quad.getY(0);
        final float z0 = quad.getZ(0);

        final float x1 = quad.getX(1);
        final float y1 = quad.getY(1);
        final float z1 = quad.getZ(1);

        final float x2 = quad.getX(2);
        final float y2 = quad.getY(2);
        final float z2 = quad.getZ(2);

        final float x3 = quad.getX(3);
        final float y3 = quad.getY(3);
        final float z3 = quad.getZ(3);

        // nx = Σ (yi - yi+1)(zi + zi+1)
        float normX = (y0 - y1) * (z0 + z1) + (y1 - y2) * (z1 + z2) + (y2 - y3) * (z2 + z3) + (y3 - y0) * (z3 + z0);

        // ny = Σ (zi - zi+1)(xi + xi+1)
        float normY = (z0 - z1) * (x0 + x1) + (z1 - z2) * (x1 + x2) + (z2 - z3) * (x2 + x3) + (z3 - z0) * (x3 + x0);

        // nz = Σ (xi - xi+1)(yi + yi+1)
        float normZ = (x0 - x1) * (y0 + y1) + (x1 - x2) * (y1 + y2) + (x2 - x3) * (y2 + y3) + (x3 - x0) * (y3 + y0);

        float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

        if (l != 0) {
            normX /= l;
            normY /= l;
            normZ /= l;
        }

        return NormI8.pack(normX, normY, normZ);
    }

    @SuppressWarnings("unused")
    public static int mergeBakedLight(int packedLight, int vanillaLightEmission, int calcLight) {
        // bail early in most cases
        if (packedLight == 0 && vanillaLightEmission == 0) return calcLight;

        int psl = (packedLight >> 16) & 0xFF;
        int csl = (calcLight >> 16) & 0xFF;
        int pbl = (packedLight) & 0xFF;
        int cbl = (calcLight) & 0xFF;
        int bl = Math.max(Math.max(pbl, cbl), vanillaLightEmission);
        int sl = Math.max(Math.max(psl, csl), vanillaLightEmission);
        return (sl << 16) | bl;
    }

    /**
     * Deep copies a list of quads. Useful when quads need to persist beyond their pool lifecycle (e.g., from
     * CapturingTessellator's object pool).
     *
     * @param sourceQuads The quads to copy
     * @return A new list containing deep copies of all quads
     */
    public static List<ModelQuadViewMutable> deepCopyQuads(List<ModelQuadViewMutable> sourceQuads) {
        final int size = sourceQuads.size();
        final List<ModelQuadViewMutable> quadCopies = new ArrayList<>(size);
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < size; i++) {
            quadCopies.add(new ModelQuad(sourceQuads.get(i)));
        }
        return quadCopies;
    }

}
