package com.gtnewhorizon.gtnhlib.client.renderer.cel.model.tri;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.COLOR_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.DEFAULT_COLOR;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.DEFAULT_LIGHTMAP;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.LIGHT_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.NORMAL_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.POSITION_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.TEXTURE_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.VERTEX_SIZE;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.vertexOffset;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive.ModelPrimitiveView;

/**
 * A triangle primitive with 3 vertices. Supports full vertex attributes including position, color, texture coordinates,
 * lightmap, and normals.
 */
@Deprecated // Replaced in favor of DirectTessellator (see TessellatorManager for more info)
public class ModelTriangle implements ModelPrimitiveView {

    private final int[] data = new int[VERTEX_SIZE * 3];
    private int computedNormal;
    private int shaderBlockId = -1;

    @Override
    public int getVertexCount() {
        return 3;
    }

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(data[vertexOffset(idx) + POSITION_INDEX]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(data[vertexOffset(idx) + POSITION_INDEX + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(data[vertexOffset(idx) + POSITION_INDEX + 2]);
    }

    @Override
    public int getColor(int idx) {
        return data[vertexOffset(idx) + COLOR_INDEX];
    }

    @Override
    public float getTexU(int idx) {
        return Float.intBitsToFloat(data[vertexOffset(idx) + TEXTURE_INDEX]);
    }

    @Override
    public float getTexV(int idx) {
        return Float.intBitsToFloat(data[vertexOffset(idx) + TEXTURE_INDEX + 1]);
    }

    @Override
    public int getLight(int idx) {
        return data[vertexOffset(idx) + LIGHT_INDEX];
    }

    @Override
    public int getForgeNormal(int idx) {
        return data[vertexOffset(idx) + NORMAL_INDEX];
    }

    // Setters

    public void setX(int idx, float x) {
        data[vertexOffset(idx) + POSITION_INDEX] = Float.floatToRawIntBits(x);
        computedNormal = 0; // Invalidate cached normal
    }

    public void setY(int idx, float y) {
        data[vertexOffset(idx) + POSITION_INDEX + 1] = Float.floatToRawIntBits(y);
        computedNormal = 0;
    }

    public void setZ(int idx, float z) {
        data[vertexOffset(idx) + POSITION_INDEX + 2] = Float.floatToRawIntBits(z);
        computedNormal = 0;
    }

    public void setColor(int idx, int color) {
        data[vertexOffset(idx) + COLOR_INDEX] = color;
    }

    public void setTexU(int idx, float u) {
        data[vertexOffset(idx) + TEXTURE_INDEX] = Float.floatToRawIntBits(u);
    }

    public void setTexV(int idx, float v) {
        data[vertexOffset(idx) + TEXTURE_INDEX + 1] = Float.floatToRawIntBits(v);
    }

    public void setLight(int idx, int light) {
        data[vertexOffset(idx) + LIGHT_INDEX] = light;
    }

    public void setForgeNormal(int idx, int normal) {
        data[vertexOffset(idx) + NORMAL_INDEX] = normal;
    }

    /**
     * Sets vertex data from a raw tessellator buffer.
     *
     * @param rawBuffer The tessellator's raw int[] buffer
     * @param srcOffset Offset to the first vertex in the source buffer
     * @param flags     Vertex format flags
     */
    public void setState(int[] rawBuffer, int srcOffset, CapturingTessellator.Flags flags) {
        System.arraycopy(rawBuffer, srcOffset, data, 0, VERTEX_SIZE * 3);

        if (!flags.hasColor) clearColors();
        if (!flags.hasNormals) clearNormals();
        if (!flags.hasBrightness) clearLightmap();

        computedNormal = 0;
    }

    /**
     * Copies a single vertex from a raw buffer directly to this triangle's data. Avoids int→float→int conversions by
     * copying raw ints directly.
     *
     * @param rawBuffer The tessellator's raw int[] buffer
     * @param srcOffset Offset to the vertex in the source buffer
     * @param destIdx   Destination vertex index (0, 1, or 2)
     */
    public void copyVertexFromBuffer(int[] rawBuffer, int srcOffset, int destIdx) {
        System.arraycopy(rawBuffer, srcOffset, data, vertexOffset(destIdx), VERTEX_SIZE);
    }

    /**
     * Calculate the face normal from the triangle's vertices.
     */
    public int getComputedFaceNormal() {
        if (computedNormal != 0) return computedNormal;

        final float x0 = getX(0), y0 = getY(0), z0 = getZ(0);
        final float x1 = getX(1), y1 = getY(1), z1 = getZ(1);
        final float x2 = getX(2), y2 = getY(2), z2 = getZ(2);

        // Cross product of (v1-v0) x (v2-v0)
        final float dx1 = x1 - x0, dy1 = y1 - y0, dz1 = z1 - z0;
        final float dx2 = x2 - x0, dy2 = y2 - y0, dz2 = z2 - z0;

        float nx = dy1 * dz2 - dz1 * dy2;
        float ny = dz1 * dx2 - dx1 * dz2;
        float nz = dx1 * dy2 - dy1 * dx2;

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len != 0) {
            nx /= len;
            ny /= len;
            nz /= len;
        }

        computedNormal = NormI8.pack(nx, ny, nz);
        return computedNormal;
    }

    private void clearColors() {
        setColor(0, DEFAULT_COLOR);
        setColor(1, DEFAULT_COLOR);
        setColor(2, DEFAULT_COLOR);
    }

    private void clearNormals() {
        computedNormal = 0;
        data[vertexOffset(0) + NORMAL_INDEX] = 0;
        data[vertexOffset(1) + NORMAL_INDEX] = 0;
        data[vertexOffset(2) + NORMAL_INDEX] = 0;
    }

    private void clearLightmap() {
        setLight(0, DEFAULT_LIGHTMAP);
        setLight(1, DEFAULT_LIGHTMAP);
        setLight(2, DEFAULT_LIGHTMAP);
    }

    public int getShaderBlockId() {
        return shaderBlockId;
    }

    public void setShaderBlockId(int shaderBlockId) {
        this.shaderBlockId = shaderBlockId;
    }

    /**
     * Applies a position offset to all vertices. More efficient than calling setX/setY/setZ individually as it
     * invalidates the computed normal only once.
     *
     * @param offsetX X offset to add to all vertices
     * @param offsetY Y offset to add to all vertices
     * @param offsetZ Z offset to add to all vertices
     */
    public void applyOffset(int offsetX, int offsetY, int offsetZ) {
        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) return;

        for (int i = 0; i < 3; i++) {
            final int offset = vertexOffset(i);
            data[offset + POSITION_INDEX] = Float
                    .floatToRawIntBits(Float.intBitsToFloat(data[offset + POSITION_INDEX]) + offsetX);
            data[offset + POSITION_INDEX + 1] = Float
                    .floatToRawIntBits(Float.intBitsToFloat(data[offset + POSITION_INDEX + 1]) + offsetY);
            data[offset + POSITION_INDEX + 2] = Float
                    .floatToRawIntBits(Float.intBitsToFloat(data[offset + POSITION_INDEX + 2]) + offsetZ);
        }
        computedNormal = 0; // Invalidate once at the end
    }

    /**
     * Copy data from another ModelTriangle.
     */
    public ModelTriangle copyFrom(ModelTriangle source) {
        System.arraycopy(source.data, 0, this.data, 0, VERTEX_SIZE * 3);
        this.computedNormal = source.computedNormal;
        this.shaderBlockId = source.shaderBlockId;
        return this;
    }
}
