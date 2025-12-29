package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

import java.nio.ByteBuffer;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public final class DirectTessellator extends Tessellator {

    private DirectDrawCallback drawCallback;
    VertexFormat format;

    final ByteBuffer baseBuffer; // never resized, never freed
    final long baseBufferAddress;
    ByteBuffer buffer; // current active buffer

    long basePtr;
    long writePtr;
    long endPtr;

    public DirectTessellator(DirectDrawCallback callback) {
        this(callback, Tessellator.byteBuffer);
    }

    public DirectTessellator(DirectDrawCallback callback, ByteBuffer initial) {
        this.drawCallback = callback;

        this.baseBuffer = initial;
        this.buffer = initial;

        this.baseBufferAddress = memAddress0(buffer);
        this.basePtr = baseBufferAddress;
        this.writePtr = basePtr;
        this.endPtr = basePtr + buffer.capacity();
    }

    @Override
    public int draw() {
        isDrawing = false;
        int count = this.vertexCount;
        if (drawCallback != null) {
            if (drawCallback.onDraw(this)) {
                this.reset();
            }
        }
        return count * 32;
    }

    int interceptDraw(Tessellator tessellator) {
        final int count = tessellator.vertexCount;
        this.vertexCount = count;
        this.hasColor = tessellator.hasColor;
        this.hasTexture = tessellator.hasTexture;
        this.hasBrightness = tessellator.hasBrightness;
        this.hasNormals = tessellator.hasNormals;
        this.isColorDisabled = tessellator.isColorDisabled;
        this.drawMode = tessellator.drawMode;
        this.format = getOptimalVertexFormat();

        writePtr = format.writeToBuffer0(writePtr, tessellator.rawBuffer, tessellator.rawBufferIndex);

        isDrawing = false;
        if (drawCallback != null) {
            if (drawCallback.onDraw(this)) {
                this.reset();
            }
        }
        return count * 32;
    }

    /**
     * Clears the tessellator state in preparation for new drawing.
     */
    @Override
    public void reset() {
        this.vertexCount = 0;
        this.isDrawing = false;
        this.hasNormals = false;
        this.hasColor = false;
        this.hasTexture = false;
        this.hasBrightness = false;
        this.isColorDisabled = false;

        this.format = null;

        if (buffer != baseBuffer) {
            memFree(buffer);
            buffer = baseBuffer;
        }

        basePtr = baseBufferAddress;
        writePtr = basePtr;
        endPtr = basePtr + buffer.capacity();
        buffer.clear();
    }

    @Override
    public void startDrawing(int p_78371_1_) {
        if (this.isDrawing) {
            throw new IllegalStateException("Already tesselating!");
        }
        this.isDrawing = true;
        this.drawMode = p_78371_1_;
    }

    private void ensureCapacity(int bytes) {
        if (writePtr + bytes <= endPtr) {
            return;
        }

        long used = getDataSize();
        int newCapacity = buffer.capacity() * 2;

        buffer = memRealloc(buffer, newCapacity);
        basePtr = memAddress0(buffer);
        writePtr = basePtr + used;
        endPtr = basePtr + newCapacity;
    }

    @Override
    public void addVertex(double x, double y, double z) {
        if (format == null) {
            setupVertexFormats(getOptimalVertexFormat());
        }

        ensureCapacity(this.format.getVertexSize());

        writePtr = format.writeToBuffer0(
                writePtr,
                this,
                (float) (x + this.xOffset),
                (float) (y + this.yOffset),
                (float) (z + this.zOffset));
        this.vertexCount++;
    }

    private void setupVertexFormats(VertexFormat format) {
        this.format = format;
        // this.endPointer = writeBuffer.capacity() - format.getVertexSize();
    }

    // If some mod does something illegal (like calling setColor after a vertex has been emitted), this will result in
    // undefined behavior, but I still have to take that into account here.
    private void fixBufferFormat() {
        final VertexFormat oldFormat = this.format;
        final VertexFormat newFormat = this.getOptimalVertexFormat();

        final int vertexCount = this.vertexCount;
        final int newVertexSize = newFormat.getVertexSize();
        final long newBufferSize = (long) vertexCount * newVertexSize;

        // Allocate temp buffer
        ByteBuffer temp = memAlloc((int) newBufferSize);
        long tempPtr = memAddress0(temp);

        long readPtr = basePtr;
        long writePtrTemp = tempPtr;

        for (int i = 0; i < vertexCount; i++) {
            float x = memGetFloat(readPtr);
            float y = memGetFloat(readPtr + 4);
            float z = memGetFloat(readPtr + 8);
            readPtr += 12;

            // Read other attributes from old format
            readPtr = oldFormat.readFromBuffer0(readPtr, this);

            // Write vertex into temporary buffer using new format
            writePtrTemp = newFormat.writeToBuffer0(writePtrTemp, this, x, y, z);
        }

        // Copy back to main buffer
        ensureCapacity((int) newBufferSize); // make sure the main buffer is large enough
        memCopy(tempPtr, basePtr, (int) newBufferSize);
        writePtr = basePtr + newBufferSize;

        memFree(temp); // free temporary buffer
        this.format = newFormat;
    }

    @Override
    public void setTextureUV(double p_78385_1_, double p_78385_3_) {
        if (!hasTexture) {
            this.hasTexture = true;
            if (format != null) {
                fixBufferFormat();
            }
        }

        this.textureU = p_78385_1_;
        this.textureV = p_78385_3_;
    }

    @Override
    public void setNormal(float p_78375_1_, float p_78375_2_, float p_78375_3_) {
        if (!hasNormals) {
            this.hasNormals = true;
            if (format != null) {
                fixBufferFormat();
            }
        }

        byte b0 = (byte) ((int) (p_78375_1_ * 127.0F));
        byte b1 = (byte) ((int) (p_78375_2_ * 127.0F));
        byte b2 = (byte) ((int) (p_78375_3_ * 127.0F));
        this.normal = b0 & 255 | (b1 & 255) << 8 | (b2 & 255) << 16;
    }

    @Override
    public void setColorRGBA(int red, int green, int blue, int alpha) {
        if (this.isColorDisabled) return;

        if (!this.hasColor) {
            this.hasColor = true;
            if (format != null) {
                fixBufferFormat();
            }
        }

        if (red > 255) {
            red = 255;
        } else if (red < 0) {
            red = 0;
        }

        if (green > 255) {
            green = 255;
        } else if (green < 0) {
            green = 0;
        }

        if (blue > 255) {
            blue = 255;
        } else if (blue < 0) {
            blue = 0;
        }

        if (alpha > 255) {
            alpha = 255;
        } else if (alpha < 0) {
            alpha = 0;
        }

        this.color = alpha << 24 | blue << 16 | green << 8 | red;
    }

    @Override
    public void setBrightness(int p_78380_1_) {
        if (!this.hasBrightness) {
            this.hasBrightness = true;
            if (format != null) {
                fixBufferFormat();
            }
        }

        this.brightness = p_78380_1_;
    }

    @Override
    public TesselatorVertexState getVertexState(float p_147564_1_, float p_147564_2_, float p_147564_3_) {
        throw new UnsupportedOperationException("getVertexState not supported for DirectTessellator!");
    }

    @Override
    public void setVertexState(TesselatorVertexState p_147565_1_) {
        throw new UnsupportedOperationException("setVertexState not supported for DirectTessellator!");
    }

    private void setBufferLimit() {
        buffer.limit(getDataSize());
    }

    int getDataSize() {
        return (int) (writePtr - basePtr);
    }

    VertexBuffer uploadToVBO() {
        setBufferLimit();
        VertexBuffer vbo = VAOManager.createVAO(this.format, this.drawMode);
        vbo.upload(buffer, this.vertexCount);
        return vbo;
    }

    /**
     * Allocates a new ByteBuffer with the contents of the tessellator's draw. <br>
     * The buffer needs to be freed with {@link com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities#memFree}
     *
     * @return The buffer copy
     */
    public ByteBuffer allocateBufferCopy() {
        final int size = getDataSize();
        ByteBuffer copy = memAlloc(size);
        memCopy(basePtr, memAddress0(copy), size);
        copy.limit(size);
        return copy;
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    public boolean isEmpty() {
        return format == null;
    }

    public VertexFormat getVertexFormat() {
        return format;
    }

    public double getLastTextureU() {
        return this.textureU;
    }

    public double getLastTextureV() {
        return this.textureV;
    }

    public int getPackedNormal() {
        return this.normal;
    }

    public int getPackedColor() {
        return this.color;
    }

    void setDrawCallback(DirectDrawCallback drawCallback) {
        this.drawCallback = drawCallback;
    }

    public VertexBuffer stopCapturingDirectToVAO() {
        return TessellatorManager.stopCapturingDirectToVAO();
    }

    private VertexFormat getOptimalVertexFormat() {
        return VertexFlags.getFormat(this);
    }
}
