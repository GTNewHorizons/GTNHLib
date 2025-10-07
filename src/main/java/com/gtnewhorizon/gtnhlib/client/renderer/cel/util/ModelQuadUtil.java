package com.gtnewhorizon.gtnhlib.client.renderer.cel.util;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.ColorARGB;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3f;

/// Provides some utilities and constants for interacting with vanilla's model quad vertex format.
/// This is the vertex format used by Minecraft 7.10 for chunk meshes and model quads. Internally, it uses integer
/// arrays for store baked quad data, and as such the following table provides both the byte and int indices.
///
/// | Byte Index | Integer Index | Name          | Format           | Fields          |
/// | ---------- | ------------- | ------------- | ---------------- | --------------- |
/// | 0..11      | 0..2          | Position      | 3 floats         | x, y, z         |
/// | 12..19     | 3..4          | Block Texture | 2 floats         | u, v            |
/// | 19..23     | 5             | Color         | 4 unsigned bytes | a, b, g, r [^1] |
/// | 24..27     | 6             | Normal        | 3 unsigned bytes | x, y, z         |
/// | 28..31     | 7             | Light Texture | 2 shorts         | u, v            |
///
/// [^1]: ABGR on little-endian systems, RGBA on big-endian systems.
public class ModelQuadUtil {
    // Integer indices for vertex attributes, useful for accessing baked quad data
    public static final int POSITION_INDEX = 0,
            COLOR_INDEX = 5,
            TEXTURE_INDEX = 3,
            LIGHT_INDEX = 7,
            NORMAL_INDEX = 6;

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
        return QuadUtil.findNormalFace(x, y, z);
    }

    public static ModelQuadFacing findNormalFace(int normal) {
        return findNormalFace(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
    }

    public static int calculateNormal(ModelQuadView quad) {
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

        final float dx0 = x2 - x0;
        final float dy0 = y2 - y0;
        final float dz0 = z2 - z0;
        final float dx1 = x3 - x1;
        final float dy1 = y3 - y1;
        final float dz1 = z3 - z1;

        float normX = dy0 * dz1 - dz0 * dy1;
        float normY = dz0 * dx1 - dx0 * dz1;
        float normZ = dx0 * dy1 - dy0 * dx1;

        float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

        if (l != 0) {
            normX /= l;
            normY /= l;
            normZ /= l;
        }

        return NormI8.pack(normX, normY, normZ);
    }

    public static void calculateNormal(ChunkVertexEncoder.Vertex[] quad, Vector3f result) {
        QuadUtil.calculateNormal(quad, result);
    }

    public static int mergeNormal(int packedNormal, int calcNormal) {
        if((packedNormal & 0xFFFFFF) == 0)
            return calcNormal;
        return packedNormal;
    }

    public static int mergeBakedLight(int packedLight, int vanillaLightEmission, int calcLight) {
        // bail early in most cases
        if (packedLight == 0 && vanillaLightEmission == 0)
            return calcLight;

        int psl = (packedLight >> 16) & 0xFF;
        int csl = (calcLight >> 16) & 0xFF;
        int pbl = (packedLight) & 0xFF;
        int cbl = (calcLight) & 0xFF;
        int bl = Math.max(Math.max(pbl, cbl), vanillaLightEmission);
        int sl = Math.max(Math.max(psl, csl), vanillaLightEmission);
        return (sl << 16) | bl;
    }

    /// Mixes two ABGR colors together like what Forge does in VertexConsumer.
    ///
    /// Despite the name, the method tries to avoid doing any work whenever possible.
    public static int mixARGBColors(int colorA, int colorB) {
        // Most common case: Either quad coloring or tint-based coloring, but not both
        if (colorA == -1) {
            return colorB;
        } else if (colorB == -1) {
            return colorA;
        }
        // General case (rare): Both colorings, actually perform the multiplication
        int a = (int)((ColorARGB.unpackAlpha(colorA)/255.0f) * (ColorARGB.unpackAlpha(colorB)/255.0f) * 255.0f);
        int b = (int)((ColorARGB.unpackBlue(colorA)/255.0f) * (ColorARGB.unpackBlue(colorB)/255.0f) * 255.0f);
        int g = (int)((ColorARGB.unpackGreen(colorA)/255.0f) * (ColorARGB.unpackGreen(colorB)/255.0f) * 255.0f);
        int r = (int)((ColorARGB.unpackRed(colorA)/255.0f) * (ColorARGB.unpackRed(colorB)/255.0f) * 255.0f);
        return ColorARGB.pack(r, g, b, a);
    }
}
