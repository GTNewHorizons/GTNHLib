package com.gtnewhorizon.gtnhlib.client.renderer.cel.model.line;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.COLOR_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.LIGHT_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.NORMAL_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.POSITION_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.TEXTURE_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.VERTEX_SIZE;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.vertexOffset;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive.ModelPrimitiveView;

/**
 * A line primitive with 2 vertices. Supports full vertex attributes including position, color, texture coordinates,
 * lightmap, and normals.
 */
@Deprecated // Replaced in favor of DirectTessellator (see TessellatorManager for more info)
public class ModelLine implements ModelPrimitiveView {

    private final int[] data = new int[VERTEX_SIZE * 2];
    private int shaderBlockId = -1;

    @Override
    public int getVertexCount() {
        return 2;
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

    // Setters for building the line

    public void setX(int idx, float x) {
        data[vertexOffset(idx) + POSITION_INDEX] = Float.floatToRawIntBits(x);
    }

    public void setY(int idx, float y) {
        data[vertexOffset(idx) + POSITION_INDEX + 1] = Float.floatToRawIntBits(y);
    }

    public void setZ(int idx, float z) {
        data[vertexOffset(idx) + POSITION_INDEX + 2] = Float.floatToRawIntBits(z);
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
     */
    public void setState(int[] rawBuffer, int srcOffset) {
        System.arraycopy(rawBuffer, srcOffset, data, 0, VERTEX_SIZE * 2);
    }

    /**
     * Copies a single vertex from a raw buffer directly to this line's data. Avoids int→float→int conversions by
     * copying raw ints directly.
     *
     * @param rawBuffer The tessellator's raw int[] buffer
     * @param srcOffset Offset to the vertex in the source buffer
     * @param destIdx   Destination vertex index (0 or 1)
     */
    public void copyVertexFromBuffer(int[] rawBuffer, int srcOffset, int destIdx) {
        System.arraycopy(rawBuffer, srcOffset, data, vertexOffset(destIdx), VERTEX_SIZE);
    }

    public int getShaderBlockId() {
        return shaderBlockId;
    }

    public void setShaderBlockId(int shaderBlockId) {
        this.shaderBlockId = shaderBlockId;
    }

    /**
     * Applies a position offset to all vertices.
     *
     * @param offsetX X offset to add to all vertices
     * @param offsetY Y offset to add to all vertices
     * @param offsetZ Z offset to add to all vertices
     */
    public void applyOffset(int offsetX, int offsetY, int offsetZ) {
        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) return;

        for (int i = 0; i < 2; i++) {
            final int offset = vertexOffset(i);
            data[offset + POSITION_INDEX] = Float
                    .floatToRawIntBits(Float.intBitsToFloat(data[offset + POSITION_INDEX]) + offsetX);
            data[offset + POSITION_INDEX + 1] = Float
                    .floatToRawIntBits(Float.intBitsToFloat(data[offset + POSITION_INDEX + 1]) + offsetY);
            data[offset + POSITION_INDEX + 2] = Float
                    .floatToRawIntBits(Float.intBitsToFloat(data[offset + POSITION_INDEX + 2]) + offsetZ);
        }
    }

    /**
     * Copy data from another ModelLine.
     */
    public ModelLine copyFrom(ModelLine source) {
        System.arraycopy(source.data, 0, this.data, 0, VERTEX_SIZE * 2);
        this.shaderBlockId = source.shaderBlockId;
        return this;
    }
}
