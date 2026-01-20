package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

import java.nio.ByteBuffer;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

/**
 * A {@link Tessellator} implementation that directly populates a ByteBuffer, which can then be used for VBO uploads.
 * <br>
 *
 */
public class DirectTessellator extends Tessellator {

    protected VertexFormat format;

    protected VertexFormat preDefinedFormat;

    protected final ByteBuffer baseBuffer; // never resized, only freed if deleteAfter is true
    protected final long baseAddress;
    protected final boolean deleteAfter;

    protected long startPtr;
    protected long writePtr;
    protected long endPtr;

    protected final int bufferCapacity() {
        return (int) (endPtr - startPtr);
    }

    protected final int bufferLimit() { // same as position
        return (int) (writePtr - startPtr);
    }

    protected final int bufferRemaining() {
        return (int) (endPtr - writePtr);
    }

    protected final boolean isResized() {
        return startPtr != baseAddress;
    }

    public DirectTessellator(ByteBuffer initial) {
        this(initial, false);
    }

    public DirectTessellator(int capacity) {
        this(memAlloc(capacity), true);
    }

    public DirectTessellator(ByteBuffer initial, boolean deleteAfter) {
        this.baseBuffer = initial;

        this.baseAddress = memAddress0(initial);
        this.startPtr = baseAddress;
        this.writePtr = startPtr;
        this.endPtr = startPtr + initial.capacity();

        this.deleteAfter = deleteAfter;
    }

    @Override
    public int draw() {
        isDrawing = false;
        return this.vertexCount * 32;
    }

    final int interceptDraw(Tessellator tessellator) {
        this.vertexCount = tessellator.vertexCount;
        this.hasColor = tessellator.hasColor;
        this.hasTexture = tessellator.hasTexture;
        this.hasBrightness = tessellator.hasBrightness;
        this.hasNormals = tessellator.hasNormals;
        this.isColorDisabled = tessellator.isColorDisabled;
        this.drawMode = tessellator.drawMode;
        this.format = preDefinedFormat != null ? preDefinedFormat : getOptimalVertexFormat();

        ensureCapacity(tessellator.rawBufferIndex);

        writePtr = format.writeToBuffer0(writePtr, tessellator.rawBuffer, tessellator.rawBufferIndex);

        return draw();
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
    public final void startDrawing(int p_78371_1_) {
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
    public final void addVertex(double x, double y, double z) {
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
    public final void setTextureUV(double p_78385_1_, double p_78385_3_) {
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
    public final void setNormal(float nx, float ny, float nz) {
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
    public final void setColorRGBA(int red, int green, int blue, int alpha) {
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
    public final void setBrightness(int p_78380_1_) {
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
    public final TesselatorVertexState getVertexState(float p_147564_1_, float p_147564_2_, float p_147564_3_) {
        throw new UnsupportedOperationException("getVertexState not supported for DirectTessellator!");
    }

    @Override
    public final void setVertexState(TesselatorVertexState p_147565_1_) {
        throw new UnsupportedOperationException("setVertexState not supported for DirectTessellator!");
    }

    /**
     * Uploads the Tessellator to a VBO.
     */
    public final IVertexBuffer uploadToVBO(VertexBufferType bufferType) {
        return bufferType.allocate(this.format, this.drawMode, getWriteBuffer(), vertexCount);
    }

    public final void updateToVBO(IVertexBuffer vbo) {
        vbo.update(getWriteBuffer());
    }

    public final void allocateToVBO(VertexBuffer vbo) {
        vbo.upload(getWriteBuffer(), this.vertexCount);
    }

    public final void updateToVBO(VertexBuffer vbo) {
        vbo.upload(getWriteBuffer());
    }

    protected ByteBuffer getWriteBuffer() {
        ByteBuffer buffer = isResized() ? memByteBuffer(startPtr, bufferCapacity()) : this.baseBuffer;
        buffer.limit(bufferLimit());
        return buffer;
    }

    public final void setVertexFormat(VertexFormat format) {
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


    /**
     * Allocates a new ByteBuffer with the contents of the tessellator's draw. <br>
     * The buffer needs to be freed with {@link com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities#memFree}
     *
     * @return The buffer copy
     */
    public final ByteBuffer allocateBufferCopy() {
        final int size = bufferLimit();
        final ByteBuffer copy = memAlloc(size);
        memCopy(startPtr, memAddress0(copy), size);
        return copy;
    }

    public final int getVertexCount() {
        return this.vertexCount;
    }

    public final boolean isEmpty() {
        return startPtr == writePtr;
    }

    public final VertexFormat getVertexFormat() {
        return format;
    }

    public final double getLastTextureU() {
        return this.textureU;
    }

    public final double getLastTextureV() {
        return this.textureV;
    }

    public final int getPackedNormal() {
        return this.normal;
    }

    public final int getPackedColor() {
        return this.color;
    }

    public final int getDrawMode() {
        return this.drawMode;
    }

    protected void onRemovedFromStack() {
        reset();
        if (this.deleteAfter) {
            delete();
        }
    }

    public void delete() {
        nmemFree(baseAddress);
    }

    public static IVertexBuffer stopCapturingToVBO(VertexBufferType bufferType) {
        return TessellatorManager.stopCapturingDirectToVBO(bufferType);
    }

    private VertexFormat getOptimalVertexFormat() {
        return VertexFlags.getFormat(this);
    }
}
