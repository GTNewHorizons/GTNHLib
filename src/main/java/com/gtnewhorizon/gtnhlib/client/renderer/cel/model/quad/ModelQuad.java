package com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.COLOR_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.DEFAULT_COLOR;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.DEFAULT_LIGHTMAP;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.LIGHT_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.NORMAL_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.POSITION_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.TEXTURE_INDEX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.VERTEX_SIZE;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.vertexOffset;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import org.jetbrains.annotations.ApiStatus;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator.Flags;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil;

/// A simple implementation of the [ModelQuadViewMutable] interface which can provide an on-heap scratch area
/// for storing quad vertex data.
public class ModelQuad implements ModelQuadViewMutable {

    private final int[] data = new int[VERTEX_SIZE * 4];
    private int flags;

    private int normal;

    private Object sprite;
    private int colorIdx;
    private ModelQuadFacing direction;

    private boolean hasAmbientOcclusion = true;
    private int shaderBlockId;
    private int emissiveness;
    private boolean dirShading;

    public ModelQuad() {}

    public ModelQuad(ModelQuadView source) {
        copyFrom(source);
    }

    @Override
    public void setX(int idx, float x) {
        this.data[vertexOffset(idx) + POSITION_INDEX] = Float.floatToRawIntBits(x);
        this.normal = 0;
    }

    @Override
    public void setY(int idx, float y) {
        this.data[vertexOffset(idx) + POSITION_INDEX + 1] = Float.floatToRawIntBits(y);
        this.normal = 0;
    }

    @Override
    public void setZ(int idx, float z) {
        this.data[vertexOffset(idx) + POSITION_INDEX + 2] = Float.floatToRawIntBits(z);
        this.normal = 0;
    }

    @Override
    public void setColor(int idx, int color) {
        this.data[vertexOffset(idx) + COLOR_INDEX] = color;
    }

    @Override
    public void setTexU(int idx, float u) {
        this.data[vertexOffset(idx) + TEXTURE_INDEX] = Float.floatToRawIntBits(u);
    }

    @Override
    public void setTexV(int idx, float v) {
        this.data[vertexOffset(idx) + TEXTURE_INDEX + 1] = Float.floatToRawIntBits(v);
    }

    @Override
    public void setLight(int idx, int light) {
        this.data[vertexOffset(idx) + LIGHT_INDEX] = light;
    }

    @Override
    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public void setSprite(Object sprite) {
        this.sprite = sprite;
    }

    @Override
    public void setColorIndex(int index) {
        this.colorIdx = index;
    }

    @Override
    public boolean setDirectionalShading(boolean dirShading) {
        return this.dirShading = dirShading;
    }

    @Override
    public int setEmissiveness(int emissiveness) {
        return this.emissiveness = emissiveness;
    }

    @Override
    public void setLightFace(ModelQuadFacing face) {
        if (!face.isDirection()) {
            throw new IllegalArgumentException();
        }
        this.direction = face;
    }

    @Override
    public void setHasAmbientOcclusion(boolean hasAmbientOcclusion) {
        this.hasAmbientOcclusion = hasAmbientOcclusion;
    }

    @Override
    public void setForgeNormal(int idx, int normal) {
        this.data[vertexOffset(idx) + NORMAL_INDEX] = normal;
    }

    @Override
    public void setShaderBlockId(int shaderBlockId) {
        this.shaderBlockId = shaderBlockId;
    }

    @Override
    public int getColorIndex() {
        return this.colorIdx;
    }

    @Override
    public boolean hasDirectionalShading() {
        return dirShading;
    }

    @Override
    public int getEmissiveness() {
        return emissiveness;
    }

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + POSITION_INDEX]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + POSITION_INDEX + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + POSITION_INDEX + 2]);
    }

    @Override
    public int getColor(int idx) {
        return this.data[vertexOffset(idx) + COLOR_INDEX];
    }

    @Override
    public float getTexU(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + TEXTURE_INDEX]);
    }

    @Override
    public float getTexV(int idx) {
        return Float.intBitsToFloat(this.data[vertexOffset(idx) + TEXTURE_INDEX + 1]);
    }

    @Override
    public int getLight(int idx) {
        return this.data[vertexOffset(idx) + LIGHT_INDEX];
    }

    @Override
    public int getForgeNormal(int idx) {
        return this.data[vertexOffset(idx) + NORMAL_INDEX];
    }

    @Override
    public int getComputedFaceNormal() {
        int n = this.normal;
        if (n == 0) {
            this.normal = n = ModelQuadUtil.calculateNormal(this);
        }
        return n;
    }

    @Override
    public ModelQuadFacing getNormalFace() {
        return ModelQuadUtil.findNormalFace(getComputedFaceNormal());
    }

    @Override
    public int getFlags() {
        return this.flags;
    }

    @Override
    public Object celeritas$getSprite() {
        return this.sprite;
    }

    @Override
    public ModelQuadFacing getLightFace() {
        return this.direction;
    }

    @Override
    public boolean hasAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }

    @Override
    public int getShaderBlockId() {
        return this.shaderBlockId;
    }

    @ApiStatus.Internal
    public void setState(int[] rawBuffer, int srcOffset, Flags flags, int drawMode, int offsetX, int offsetY,
            int offsetZ) {
        System.arraycopy(rawBuffer, srcOffset, data, 0, data.length);

        this.normal = 0;

        if (!flags.hasColor) clearColors();
        if (!flags.hasNormals) this.clearNormals();
        if (!flags.hasBrightness) this.clearLightmap();
        // TODO confirm this is correct
        setHasAmbientOcclusion(flags.hasBrightness);

        offsetPos(0, offsetX);
        offsetPos(1, offsetY);
        offsetPos(2, offsetZ);

        if (drawMode == GL_TRIANGLES) quadrangulate();
    }

    /// Copies the third vertex to the fourth, turning this into a degenerate quad. Useful for faking triangle support.
    private void quadrangulate() {
        System.arraycopy(data, vertexOffset(2), data, vertexOffset(3), VERTEX_SIZE);
    }

    /// Shifts every vertex by the given amount.
    /// @param idx The index of the position coord to offset. 0 = x, 1 = y, 2 = z.
    /// @param offset The amount to shift the coord by.
    private void offsetPos(int idx, float offset) {
        final int i = POSITION_INDEX + idx;
        setData(0, i, getData(0, i) + offset);
        setData(1, i, getData(1, i) + offset);
        setData(2, i, getData(2, i) + offset);
        setData(3, i, getData(3, i) + offset);
    }

    private float getData(int vi, int i) {
        return Float.intBitsToFloat(data[vertexOffset(vi) + i]);
    }

    private void setData(int vi, int i, float val) {
        data[vertexOffset(vi) + i] = Float.floatToIntBits(val);
    }

    private void clearColors() {
        setColor(0, DEFAULT_COLOR);
        setColor(1, DEFAULT_COLOR);
        setColor(2, DEFAULT_COLOR);
        setColor(3, DEFAULT_COLOR);
    }

    private void clearNormals() {
        normal = 0;
        data[vertexOffset(0) + NORMAL_INDEX] = 0;
        data[vertexOffset(1) + NORMAL_INDEX] = 0;
        data[vertexOffset(2) + NORMAL_INDEX] = 0;
        data[vertexOffset(3) + NORMAL_INDEX] = 0;
    }

    private void clearLightmap() {
        setLight(0, DEFAULT_LIGHTMAP);
        setLight(1, DEFAULT_LIGHTMAP);
        setLight(2, DEFAULT_LIGHTMAP);
        setLight(3, DEFAULT_LIGHTMAP);
    }

    /**
     * Deep copies all data from the source quad into this quad. Used when quads need to be retained beyond their pool
     * lifecycle.
     *
     * @param source The quad to copy from
     */
    public ModelQuad copyFrom(ModelQuadView source) {
        // Optimize for ModelQuad-to-ModelQuad copies
        if (source instanceof ModelQuad sourceQuad) {
            System.arraycopy(sourceQuad.data, 0, this.data, 0, VERTEX_SIZE * 4);
            this.flags = sourceQuad.flags;
            this.normal = sourceQuad.normal;
            this.sprite = sourceQuad.sprite;
            this.colorIdx = sourceQuad.colorIdx;
            this.direction = sourceQuad.direction;
            this.hasAmbientOcclusion = sourceQuad.hasAmbientOcclusion;
            this.shaderBlockId = sourceQuad.shaderBlockId;
        } else {
            // Fallback for generic ModelQuadView sources - copy element by element
            for (int i = 0; i < 4; i++) {
                setX(i, source.getX(i));
                setY(i, source.getY(i));
                setZ(i, source.getZ(i));
                setColor(i, source.getColor(i));
                setTexU(i, source.getTexU(i));
                setTexV(i, source.getTexV(i));
                setLight(i, source.getLight(i));
                setForgeNormal(i, source.getForgeNormal(i));
            }

            // Copy metadata
            this.flags = source.getFlags();
            this.normal = source.getComputedFaceNormal();
            this.sprite = source.celeritas$getSprite();
            this.colorIdx = source.getColorIndex();
            this.direction = source.getLightFace();
            this.hasAmbientOcclusion = source.hasAmbientOcclusion();
            this.shaderBlockId = source.getShaderBlockId();
        }
        return this;
    }
}
