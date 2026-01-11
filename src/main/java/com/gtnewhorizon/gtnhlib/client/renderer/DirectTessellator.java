package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferStorage;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

/**
 * A {@link Tessellator} implementation that directly populates a ByteBuffer, which can then be used for VBO uploads.
 * <br>
 *
 */
public final class DirectTessellator extends Tessellator {

    private DirectDrawCallback drawCallback;
    private VertexFormat format;

    private VertexFormat preDefinedFormat;

    final ByteBuffer baseBuffer; // never resized, never freed
    final long baseAddress;

    long startPtr;
    long writePtr;
    long endPtr;

    int bufferCapacity() {
        return (int) (endPtr - startPtr);
    }

    int bufferLimit() { // same as position
        return (int) (writePtr - startPtr);
    }

    int bufferRemaining() {
        return (int) (endPtr - writePtr);
    }

    boolean isResized() {
        return startPtr != baseAddress;
    }

    public DirectTessellator(ByteBuffer initial, DirectDrawCallback callback) {
        this(initial);
        this.drawCallback = callback;
    }

    public DirectTessellator(ByteBuffer initial) {
        this(initial, false);
    }

    public DirectTessellator(ByteBuffer initial, boolean deleteAfter) {
        this.baseBuffer = initial;

        this.baseAddress = memAddress0(initial);
        this.startPtr = baseAddress;
        this.writePtr = startPtr;
        this.endPtr = startPtr + initial.capacity();

        this.defaultTexture = deleteAfter; // defaultTexture has no use, so might aswell use it to prevent allocations
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
        this.format = preDefinedFormat != null ? preDefinedFormat : getOptimalVertexFormat();

        ensureCapacity(tessellator.rawBufferIndex);

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
        this.preDefinedFormat = null;

        if (isResized()) {
            nmemFree(startPtr);
            startPtr = baseAddress;
            endPtr = startPtr + baseBuffer.capacity();
        }

        writePtr = startPtr;
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
        if (bufferRemaining() >= bytes) {
            return;
        }

        final long used = bufferLimit();
        final int newCapacity = bufferCapacity() * 2;

        startPtr = nmemReallocChecked(startPtr, newCapacity);
        writePtr = startPtr + used;
        endPtr = startPtr + newCapacity;
    }

    @Override
    public void addVertex(double x, double y, double z) {
        if (format == null) {
            this.format = getOptimalVertexFormat();
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

        long readPtr = startPtr;
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
        memCopy(tempPtr, startPtr, (int) newBufferSize);
        writePtr = startPtr + newBufferSize;

        memFree(temp); // free temporary buffer
        this.format = newFormat;
    }

    @Override
    public void setTextureUV(double p_78385_1_, double p_78385_3_) {
        if (!hasTexture) {
            if (preDefinedFormat != null) return;

            this.hasTexture = true;

            if (format != null) {
                fixBufferFormat();
            }
        }

        this.textureU = p_78385_1_;
        this.textureV = p_78385_3_;
    }

    @Override
    public void setNormal(float nx, float ny, float nz) {
        if (!hasNormals) {
            if (preDefinedFormat != null) return;

            this.hasNormals = true;

            if (format != null) {
                fixBufferFormat();
            }
        }

        byte b0 = (byte) ((int) (nx * 127.0F));
        byte b1 = (byte) ((int) (ny * 127.0F));
        byte b2 = (byte) ((int) (nz * 127.0F));
        this.normal = b0 & 255 | (b1 & 255) << 8 | (b2 & 255) << 16;
    }

    @Override
    public void setColorRGBA(int red, int green, int blue, int alpha) {
        if (this.isColorDisabled) return;

        if (!this.hasColor) {
            if (preDefinedFormat != null) return;

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
            if (preDefinedFormat != null) return;

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

    /**
     * Uploads the Tessellator to a VBO.
     */
    public IVertexBuffer uploadToVBO(VertexBufferType bufferType) {
        return bufferType.allocate(this.format, this.drawMode, getWriteBuffer(), vertexCount);
    }

    public void uploadToVBO(IVertexBuffer vbo) {
        vbo.update(getWriteBuffer());
    }

    ByteBuffer getWriteBuffer() {
        ByteBuffer buffer = isResized() ? memByteBuffer(startPtr, bufferCapacity()) : this.baseBuffer;
        buffer.limit(bufferLimit());
        return buffer;
    }

    /**
     * Allocates a new ByteBuffer with the contents of the tessellator's draw. <br>
     * The buffer needs to be freed with {@link com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities#memFree}
     *
     * @return The buffer copy
     */
    public ByteBuffer allocateBufferCopy() {
        final int size = bufferLimit();
        ByteBuffer copy = memAlloc(size);
        memCopy(startPtr, memAddress0(copy), size);
        return copy;
    }

    public void setVertexFormat(VertexFormat format) {
        if (this.format != null) {
            throw new IllegalStateException("Cannot call setVertexFormat() after a vertex has already been emitted!");
        }
        this.preDefinedFormat = format;
        this.format = format;
        this.hasTexture = format.hasTexture();
        this.hasNormals = format.hasNormals();
        this.hasBrightness = format.hasBrightness();
        this.hasColor = format.hasColor();
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    public boolean isEmpty() {
        return startPtr == writePtr;
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

    public void setDrawCallback(DirectDrawCallback drawCallback) {
        this.drawCallback = drawCallback;
    }

    void onRemovedFromStack() {
        reset();
        if (this.defaultTexture) {
            nmemFree(baseAddress);
        }
    }

    public IVertexBuffer stopCapturingToVBO(VertexBufferType bufferType) {
        return TessellatorManager.stopCapturingDirectToVBO(bufferType);
    }

    public void stopCapturingToVBO(IVertexBuffer vbo) {
        TessellatorManager.stopCapturingDirectToVBO(vbo);
    }

    private VertexFormat getOptimalVertexFormat() {
        return VertexFlags.getFormat(this);
    }
}
