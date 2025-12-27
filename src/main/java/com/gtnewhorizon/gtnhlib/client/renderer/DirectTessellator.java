package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement;

public final class DirectTessellator extends Tessellator implements AutoCloseable {

    private DirectDrawCallback drawCallback;
    private VertexFormat format;

    private int textureOffset;
    private int normalOffset;
    private int brightnessOffset;
    private int colorOffset;

    private long pointer;
    private final long address;
    private final ByteBuffer buffer;

    // The copy of the main buffer (populated when calling getBufferCopy) or when the original buffer doesn't have enough space
    // Needs to be cleared after reset();
    private ByteBuffer copyBuffer;

    public DirectTessellator(DirectDrawCallback callback) {
        this(callback, Tessellator.byteBuffer);
    }

    public DirectTessellator(DirectDrawCallback callback, ByteBuffer buffer) {
        this.drawCallback = callback;
        this.buffer = buffer;
        this.address = memAddress0(buffer);
        this.pointer = address;
    }

    /**
     * Draws the data set up in this tessellator and resets the state to prepare for new drawing.
     */
    public int draw() {
        isDrawing = false;
        if (drawCallback != null) {
            if (drawCallback.onDraw(this)) {
                this.reset();
            }
        }
        return this.vertexCount * 32;
    }

    public int interceptDraw(Tessellator tessellator) {
        this.vertexCount = tessellator.vertexCount;
        this.addedVertices = tessellator.addedVertices;
        this.hasColor = tessellator.hasColor;
        this.hasTexture = tessellator.hasTexture;
        this.hasBrightness = tessellator.hasBrightness;
        this.hasNormals = tessellator.hasNormals;
        this.isColorDisabled = tessellator.isColorDisabled;
        this.drawMode = tessellator.drawMode;
        this.format = getOptimalVertexFormat();


        pointer = format.writeToBuffer0(pointer, tessellator.rawBuffer, tessellator.rawBufferIndex);

        isDrawing = false;
        if (drawCallback != null) {
            if (drawCallback.onDraw(this)) {
                this.reset();
            }
        }
        return this.vertexCount * 32;
    }

    private void clearBuffer() {
        this.buffer.clear();
        this.pointer = address;
    }

    /**
     * Clears the tessellator state in preparation for new drawing.
     */
    @Override
    public void reset() {
        this.vertexCount = 0;
        this.addedVertices = 0;
        this.isDrawing = false;
        this.hasNormals = false;
        this.hasColor = false;
        this.hasTexture = false;
        this.hasBrightness = false;
        this.isColorDisabled = false;

        this.format = null;
        if (copyBuffer != null) {
            memFree(copyBuffer);
            copyBuffer = null;
        }
        buffer.clear();
        resetPosition();
    }

    private void resetPosition() {
        this.pointer = address;
    }

    private ByteBuffer getReadBuffer() {
        this.buffer.position((int) (this.pointer - this.address));
        this.buffer.flip();
        return this.buffer;
    }

    private ByteBuffer getWriteBuffer() {
        this.buffer.position((int) (this.pointer - this.address));
        return this.buffer;
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
    }

    @Override
    public void addVertex(double x, double y, double z) {
        if (format == null) {
            setupVertexFormats(getOptimalVertexFormat());
        }
        // ensureBuffer(); TODO

        final long pointer = this.pointer;
        memPutFloat(pointer, (float) (x + this.xOffset));
        memPutFloat(pointer + 4, (float) (y + this.yOffset));
        memPutFloat(pointer + 8, (float) (z + this.zOffset));

        if (this.hasTexture) {
            memPutFloat(pointer + textureOffset, (float) this.textureU);
            memPutFloat(pointer + textureOffset + 4, (float) this.textureV);
        }

        if (this.hasBrightness) {
            memPutInt(pointer + brightnessOffset, this.brightness);
        }

        if (this.hasColor) {
            memPutInt(pointer + colorOffset, this.color);
        }

        if (this.hasNormals) {
            memPutInt(pointer + normalOffset, this.normal);
        }

        this.pointer = pointer + format.getVertexSize();

        this.addedVertices++;
        this.vertexCount++;
    }

//    private void ensureCapacity() {
//        if (this.buffer.capacity() < (addedVertices + 1) * ) {
//
//        }
//    }

    private void setupVertexFormats(VertexFormat format) {
        this.format = format;
        final List<VertexFormatElement> elements = format.getElements();
        final int size = elements.size();
        int offset = 12; // 12 for the position
        for (int i = 1; i < size; i++) {
            final VertexFormatElement element = elements.get(i);
            switch (element.getVertexBit()) {
                case VertexFlags.TEXTURE_BIT -> {
                    textureOffset = offset;
                    offset += 8;
                }
                case VertexFlags.COLOR_BIT -> {
                    colorOffset = offset;
                    offset += 4;
                }
                case VertexFlags.NORMAL_BIT -> {
                    normalOffset = offset;
                    offset += 4;
                }
                case VertexFlags.BRIGHTNESS_BIT -> {
                    brightnessOffset = offset;
                    offset += 4;
                }
            }
        }
    }

    // If some mod does something illegal (like calling setColor after a vertex has been emitted), this will result in
    // Unintended behaviour, but I still have to take that into account here.
    private void fixBufferFormat(boolean hasTexture, boolean hasColor, boolean hasNormals, boolean hasBrightness) {
        final boolean oldHasTexture = this.hasTexture;
        final boolean oldHasColor = this.hasColor;
        final boolean oldHasNormals = this.hasNormals;
        final boolean oldHasBrightness = this.hasBrightness;

        final int oldTextureOffset = this.textureOffset;
        final int oldColorOffset = this.colorOffset;
        final int oldNormalOffset = this.normalOffset;
        final int oldBrightnessOffset = this.brightnessOffset;

        this.hasTexture = hasTexture;
        this.hasColor = hasColor;
        this.hasNormals = hasNormals;
        this.hasBrightness = hasBrightness;

        throw new UnsupportedOperationException();

        // this.format = getOptimalVertexFormat();
        // setupVertexFormats(format);
        //
        // ByteBuffer old = this.getBufferCopy();
        // long oldPtr = memAddress0(old);

        // if (this.hasTexture) {
        // if (oldHasTexture) {
        // memPutFloat(pointer + textureOffset, memGetFloat(oldPtr + ));
        // memPutFloat(pointer + textureOffset + 4, (float) this.textureV);
        // }
        //
        // }
        //
        // if (this.hasBrightness) {
        // memPutInt(pointer + brightnessOffset, this.brightness);
        // }
        //
        // if (this.hasColor) {
        // memPutInt(pointer + colorOffset, this.color);
        // }
        //
        // if (this.hasNormals) {
        // memPutInt(pointer + normalOffset, this.normal);
        // }
    }

    @Override
    public void setTextureUV(double p_78385_1_, double p_78385_3_) {
        if (format == null) {
            this.hasTexture = true;
        } else if (!this.hasTexture) {
            fixBufferFormat(true, this.hasColor, this.hasNormals, this.hasBrightness);
        }

        this.textureU = p_78385_1_;
        this.textureV = p_78385_3_;
    }

    @Override
    public void setNormal(float p_78375_1_, float p_78375_2_, float p_78375_3_) {
        if (format == null) {
            this.hasNormals = true;
        } else if (!this.hasNormals) {
            fixBufferFormat(this.hasTexture, this.hasColor, true, this.hasBrightness);
        }
        byte b0 = (byte) ((int) (p_78375_1_ * 127.0F));
        byte b1 = (byte) ((int) (p_78375_2_ * 127.0F));
        byte b2 = (byte) ((int) (p_78375_3_ * 127.0F));
        this.normal = b0 & 255 | (b1 & 255) << 8 | (b2 & 255) << 16;
    }

    @Override
    public void setColorRGBA(int red, int green, int blue, int alpha) {
        if (this.isColorDisabled) return;
        if (red > 255) {
            red = 255;
        }

        if (green > 255) {
            green = 255;
        }

        if (blue > 255) {
            blue = 255;
        }

        if (alpha > 255) {
            alpha = 255;
        }

        if (red < 0) {
            red = 0;
        }

        if (green < 0) {
            green = 0;
        }

        if (blue < 0) {
            blue = 0;
        }

        if (alpha < 0) {
            alpha = 0;
        }

        if (format == null) {
            this.hasColor = true;
        } else if (!this.hasColor) {
            fixBufferFormat(this.hasTexture, true, this.hasNormals, this.hasBrightness);
        }

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            this.color = alpha << 24 | blue << 16 | green << 8 | red;
        } else {
            this.color = red << 24 | green << 16 | blue << 8 | alpha;
        }
    }

    @Override
    public void setBrightness(int p_78380_1_) {
        if (format == null) {
            this.hasBrightness = true;
        } else if (!this.hasBrightness) {
            fixBufferFormat(this.hasTexture, this.hasColor, this.hasNormals, true);
        }
        this.brightness = p_78380_1_;
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    VertexBuffer uploadToVBO() {
        VertexBuffer vbo = VAOManager.createVAO(this.format, this.drawMode);
        ByteBuffer buffer = this.getReadBuffer();
        vbo.upload(buffer, this.vertexCount);
        this.reset();
        return vbo;
    }

    public ByteBuffer getBufferCopy() {
        ByteBuffer buffer = this.getReadBuffer();
        final int remaining = buffer.limit();
        ByteBuffer copy = memAlloc(remaining);
        memCopy(buffer, copy);
        copy.position(remaining);
        copy.flip();
        return copy;
    }

//    private VertexFormat getOptimalVertexFormat() {
//        return VertexFlags.getFormat(this);
//    }

    public boolean isEmpty() {
        return format == null;
    }

    public VertexFormat getVertexFormat() {
        return format;
    }

    @Override
    public TesselatorVertexState getVertexState(float p_147564_1_, float p_147564_2_, float p_147564_3_) {
        throw new UnsupportedOperationException("getVertexState not supported for DirectTessellator!");
    }

    @Override
    public void setVertexState(TesselatorVertexState p_147565_1_) {
        throw new UnsupportedOperationException("setVertexState not supported for DirectTessellator!");
    }

    public VertexFormat getOptimalVertexFormat() {
        return VertexFlags.getFormat(this);
    }

    @Override
    public void close() {
        reset();
    }
}
