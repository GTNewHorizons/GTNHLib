package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;
import static net.minecraft.util.MathHelper.clamp_int;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;

import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public final class DirectTessellator extends Tessellator {

    private static final DirectTessellator mainInstance = new DirectTessellator(null, 0x20000);

    private static final int VERTEX_SIZE_INT = 8;

    public static DirectTessellator reuseMainInstance() {
        return reuseMainInstance(null);
    }

    public static DirectTessellator reuseMainInstance(DirectDrawCallback callback) {
        return mainInstance.reuse(callback);
    }

    public DirectTessellator reuse(DirectDrawCallback callback) {
        this.drawCallback = callback;
        reset();
        return this;
    }

    public DirectDrawCallback drawCallback;

    public DirectTessellator(DirectDrawCallback callback) {
        this(callback, 512 * VERTEX_SIZE_INT);
    }

    public DirectTessellator(DirectDrawCallback callback, int capacity) {
        this.drawCallback = callback;
        this.rawBufferSize = capacity;
        this.rawBuffer = new int[capacity];
    }

    public void expandBuffer() {
        System.out.println("Increasing buffer from " + rawBufferSize + " to " + (rawBufferSize << 1));
        rawBufferSize = (rawBufferSize << 1);
        rawBuffer = Arrays.copyOf(rawBuffer, rawBufferSize);
    }

    /**
     * Draws the data set up in this tessellator and resets the state to prepare for new drawing.
     */
    public int draw() {
        if (!this.isDrawing) throw new IllegalStateException("Not tesselating!");
        isDrawing = false;
        final int i = this.rawBufferIndex * 4;
        if (drawCallback != null) {
            drawCallback.onDraw(this);
        }
        return i;
    }

    @Deprecated
    public void discard() {
        reset();
    }

    public void set(Tessellator tessellator) {
        this.isDrawing = true;

        this.rawBufferSize = tessellator.rawBufferSize;

        this.rawBuffer = Arrays.copyOf(tessellator.rawBuffer, tessellator.rawBuffer.length);
        this.vertexCount = tessellator.vertexCount;
        this.textureU = tessellator.textureU;
        this.textureV = tessellator.textureV;
        this.brightness = tessellator.brightness;
        this.color = tessellator.color;
        this.hasColor = tessellator.hasColor;
        this.hasTexture = tessellator.hasTexture;
        this.hasBrightness = tessellator.hasBrightness;
        this.hasNormals = tessellator.hasNormals;
        this.rawBufferIndex = tessellator.rawBufferIndex;
        this.addedVertices = tessellator.addedVertices;
        this.isColorDisabled = tessellator.isColorDisabled;
        this.drawMode = tessellator.drawMode;
        this.xOffset = tessellator.xOffset;
        this.yOffset = tessellator.yOffset;
        this.zOffset = tessellator.zOffset;
        this.normal = tessellator.normal;
    }

    @Override
    public TesselatorVertexState getVertexState(float p_147564_1_, float p_147564_2_, float p_147564_3_) {
        throw new UnsupportedOperationException("getVertexState not supported for DirectTessellator!");
    }

    @Override
    public void setVertexState(TesselatorVertexState p_147565_1_) {
        throw new UnsupportedOperationException("setVertexState not supported for DirectTessellator!");
    }

    /**
     * Clears the tessellator state in preparation for new drawing.
     */
    @Override
    public void reset() {
        this.vertexCount = 0;
        this.rawBufferIndex = 0;
        this.addedVertices = 0;
        this.isDrawing = false;
    }

    /**
     * Resets tessellator state and prepares for drawing (with the specified draw mode).
     */
    @Override
    public void startDrawing(int p_78371_1_) {
        if (this.isDrawing) {
            throw new IllegalStateException("Already tesselating!");
        }
        this.isDrawing = true;
        this.drawMode = p_78371_1_;
        this.hasNormals = false;
        this.hasColor = false;
        this.hasTexture = false;
        this.hasBrightness = false;
        this.isColorDisabled = false;
    }

    /**
     * Adds a vertex with the specified x,y,z to the current draw call. It will trigger a draw() if the buffer gets
     * full.
     */
    @Override
    public void addVertex(double p_78377_1_, double p_78377_3_, double p_78377_5_) {
        ensureBuffer();

        if (this.hasTexture) {
            this.rawBuffer[this.rawBufferIndex | TEX_X_INDEX] = Float.floatToRawIntBits((float) this.textureU);
            this.rawBuffer[this.rawBufferIndex | TEX_Y_INDEX] = Float.floatToRawIntBits((float) this.textureV);
        }

        if (this.hasBrightness) {
            this.rawBuffer[this.rawBufferIndex | LIGHT_INDEX] = this.brightness;
        }

        if (this.hasColor) {
            this.rawBuffer[this.rawBufferIndex | COLOR_INDEX] = this.color;
        }

        if (this.hasNormals) {
            this.rawBuffer[this.rawBufferIndex | NORMAL_INDEX] = this.normal;
        }

        this.rawBuffer[this.rawBufferIndex | X_INDEX] = Float.floatToRawIntBits((float) (p_78377_1_ + this.xOffset));
        this.rawBuffer[this.rawBufferIndex | Y_INDEX] = Float.floatToRawIntBits((float) (p_78377_3_ + this.yOffset));
        this.rawBuffer[this.rawBufferIndex | Z_INDEX] = Float.floatToRawIntBits((float) (p_78377_5_ + this.zOffset));
        this.rawBufferIndex += 8;
    }

    public VertexFormat getOptimalVertexFormat() {
        final boolean hasColor = this.hasColor;
        final boolean hasTexture = this.hasTexture;
        final boolean hasBrightness = this.hasBrightness;
        final boolean hasNormals = this.hasNormals;

        final VertexFormat format;

        // Map flags to GTNHLib formats, ordered by guess at frequency/likelihood
        if (!hasColor && hasTexture && !hasBrightness && !hasNormals) {
            format = DefaultVertexFormat.POSITION_TEXTURE; // Entity models, common
        } else if (!hasColor && !hasTexture && !hasBrightness && !hasNormals) {
            format = DefaultVertexFormat.POSITION; // Sky rendering, simple geometry
        } else if (hasColor && hasTexture && !hasBrightness && !hasNormals) {
            format = DefaultVertexFormat.POSITION_TEXTURE_COLOR; // Colored blocks/UI
        } else if (!hasColor && hasTexture && hasBrightness && hasNormals) {
            format = DefaultVertexFormat.POSITION_TEXTURE_LIGHT_NORMAL; // BuildCraft fluids
        } else if (!hasColor && hasTexture && !hasBrightness && hasNormals) {
            format = DefaultVertexFormat.POSITION_TEXTURE_NORMAL; // Lit geometry with normals
        } else if (hasColor && hasTexture && hasBrightness && !hasNormals) {
            format = DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP; // Colored lit blocks
        } else if (hasColor && !hasTexture && !hasBrightness && !hasNormals) {
            format = DefaultVertexFormat.POSITION_COLOR; // Colored lines/particles
        } else {
            format = DefaultVertexFormat.POSITION_COLOR_TEXTURE_LIGHT_NORMAL; // Full format fallback
        }

        return format;
    }

    public ByteBuffer compileToByteBuffer(VertexFormat format) {
        ByteBuffer buffer = byteBuffer;
        buffer.clear();

        format.writeToBuffer(buffer, rawBuffer, rawBufferIndex);
        buffer.flip();

        // System.out.println(
        // "Number vertices: " + (rawBufferIndex / 8)
        // + ", calculated vertices: "
        // + (buffer.remaining() / format.getVertexSize())
        // + ", new calculated vertices: " + (buffer.limit() / format.getVertexSize()));
        return buffer;
    }

    public ByteBuffer compileToByteBufferCopy(VertexFormat format) {
        ByteBuffer buffer = BufferUtils.createByteBuffer((rawBufferIndex / 8) * format.getVertexSize());
        buffer.clear();

        format.writeToBuffer(buffer, rawBuffer, rawBufferIndex);
        buffer.flip();
        return buffer;
    }

    public int[] getVertexData() {
        return Arrays.copyOf(rawBuffer, rawBufferIndex);
    }

    public void ensureBuffer() {
        if (rawBufferIndex >= rawBufferSize - 32) {
            expandBuffer();
        }
    }

    // CAPTURING TESSELLATOR METHODS

    public DirectTessellator pos(double x, double y, double z) {
        ensureBuffer();

        this.rawBuffer[this.rawBufferIndex | X_INDEX] = Float.floatToRawIntBits((float) (x + this.xOffset));
        this.rawBuffer[this.rawBufferIndex | Y_INDEX] = Float.floatToRawIntBits((float) (y + this.yOffset));
        this.rawBuffer[this.rawBufferIndex | Z_INDEX] = Float.floatToRawIntBits((float) (z + this.zOffset));

        return this;
    }

    public DirectTessellator tex(double u, double v) {
        this.rawBuffer[this.rawBufferIndex | TEX_X_INDEX] = Float.floatToRawIntBits((float) u);
        this.rawBuffer[this.rawBufferIndex | TEX_Y_INDEX] = Float.floatToRawIntBits((float) v);
        this.hasTexture = true;

        return this;
    }

    public DirectTessellator color(float red, float green, float blue, float alpha) {
        return this.color((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
    }

    public DirectTessellator color(int red, int green, int blue, int alpha) {
        if (this.isColorDisabled) return this;
        red = clamp_int(red, 0, 255);
        green = clamp_int(green, 0, 255);
        blue = clamp_int(blue, 0, 255);
        alpha = clamp_int(alpha, 0, 255);

        final int color;
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            color = alpha << 24 | blue << 16 | green << 8 | red;
        } else {
            color = red << 24 | green << 16 | blue << 8 | alpha;
        }
        this.rawBuffer[this.rawBufferIndex | COLOR_INDEX] = color;
        this.hasColor = true;

        return this;
    }

    public DirectTessellator normal(float x, float y, float z) {
        final byte b0 = (byte) ((int) (x * 127.0F));
        final byte b1 = (byte) ((int) (y * 127.0F));
        final byte b2 = (byte) ((int) (z * 127.0F));

        this.rawBuffer[this.rawBufferIndex | NORMAL_INDEX] = b0 & 255 | (b1 & 255) << 8 | (b2 & 255) << 16;
        this.hasNormals = true;
        return this;
    }

    public DirectTessellator setNormal(int normal) {
        this.hasNormals = true;
        this.normal = normal;
        return this;
    }

    /**
     * Sets the normal based on a given normal matrix
     *
     * @param normal       The normal vector
     * @param dest         The vector that gets transformed
     * @param normalMatrix The normal matrix (typically the transpose of the inverse transformation matrix)
     */
    public DirectTessellator setNormalTransformed(Vector3f normal, Vector3f dest, Matrix3f normalMatrix) {
        normalMatrix.transform(normal, dest).normalize();
        this.setNormal(dest.x, dest.y, dest.z);
        return this;
    }

    /**
     * Same as the method above, but this one will mutate the passed Vector3f
     */
    public DirectTessellator setNormalTransformed(Vector3f normal, Matrix3f normalMatrix) {
        return setNormalTransformed(normal, normal, normalMatrix);
    }

    public DirectTessellator lightmap(int skyLight, int blockLight) {
        return brightness(CapturingTessellator.createBrightness(skyLight, blockLight));
    }

    public DirectTessellator brightness(int brightness) {
        this.rawBuffer[this.rawBufferIndex | LIGHT_INDEX] = brightness;
        this.hasBrightness = true;

        return this;
    }

    public DirectTessellator endVertex() {
        this.rawBufferIndex += 8;

        return this;
    }

    public boolean isEmpty() {
        return rawBufferSize == 0;
    }
}
