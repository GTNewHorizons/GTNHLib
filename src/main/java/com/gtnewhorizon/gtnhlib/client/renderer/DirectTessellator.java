package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

import java.nio.ByteBuffer;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.TesselatorVertexState;

import org.lwjgl.opengl.GL11;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IndexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexOptimizer;

/**
 * A {@link Tessellator} implementation that directly populates a ByteBuffer, which can then be used for VBO uploads.
 * <br>
 *
 */
public class DirectTessellator extends Tessellator {

    protected VertexFormat format;

    @Deprecated // Idk what to do with this, this has caused me too much headache
    protected VertexFormat preDefinedFormat;

    protected final ByteBuffer baseBuffer; // never resized, only freed if deleteAfter is true
    protected final long baseAddress;
    protected final boolean deleteAfter;

    protected long startPtr;
    protected long writePtr;
    protected long endPtr;

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
        // Note that this does not represent the actual byte size of the data,
        // but rather it returns the same as Tessellator.draw() would
        return this.vertexCount * 32;
    }

    public final void syncVanillaTessellator() {
        syncTessellator(TessellatorManager.getVanillaTessellator());
    }

    public final void syncTessellator(Tessellator other) {
        if (!other.isDrawing || other.vertexCount == 0) return; // Not drawing, nothing to sync

        // Cannot merge when both tessellators are drawing with different draw mode
        if (this.isDrawing && other.drawMode != this.drawMode) {
            TessellatorManager.LOGGER.error(
                    "Failed draw merging between 2 Tessellators due to mismatching draw mode! Discarding data.",
                    new IllegalStateException());
            return;
        }

        // If this tessellator has emitted a vertex, try to merge their data together
        // Really scuffed, but should cover some rare edge-cases

        this.isDrawing = true;
        this.vertexCount = this.vertexCount + other.vertexCount;
        this.isColorDisabled = other.isColorDisabled;

        if (other.hasColor) {
            this.setColorRGBA_I(other.color, other.color >> 24);
        }
        if (other.hasTexture) {
            this.setTextureUV(other.textureU, other.textureV);
        }
        if (other.hasBrightness) {
            this.setBrightness(other.brightness);
        }
        if (other.hasNormals) {
            final int normal = other.normal;
            byte b0 = (byte) (normal & 0xFF);
            byte b1 = (byte) ((normal >> 8) & 0xFF);
            byte b2 = (byte) ((normal >> 16) & 0xFF);
            this.setNormal(b0, b1, b2);
        }

        this.format = preDefinedFormat != null ? preDefinedFormat : getOptimalVertexFormat();

        ensureCapacity(other.vertexCount * format.getVertexSize());

        writePtr = writeVertexData(format, other.rawBuffer, other.rawBufferIndex);

        TessellatorManager.discardTessellator(other);

    }

    protected int interceptDraw(Tessellator tessellator) {
        syncTessellator(tessellator);
        return draw();
    }

    protected long writeVertexData(VertexFormat format, int[] rawBuffer, int rawBufferIndex) {
        return format.writeToBuffer0(writePtr, rawBuffer, rawBufferIndex);
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
        reset();
        this.isDrawing = true;
        this.drawMode = p_78371_1_;
    }

    protected final void ensureCapacity(int bytes) {
        if (bufferRemaining() >= bytes) {
            return;
        }

        final long used = bufferLimit();

        int newCapacity = bufferCapacity() * 2;
        long required = used + bytes;

        while (newCapacity < required) {
            newCapacity *= 2;
        }

        if (!isResized()) {
            long newPtr = nmemAllocChecked(newCapacity);
            memCopy(startPtr, newPtr, used);
            startPtr = newPtr;
        } else {
            startPtr = nmemReallocChecked(startPtr, newCapacity);
        }

        writePtr = startPtr + used;
        endPtr = startPtr + newCapacity;
    }

    @Override
    public void addVertex(double x, double y, double z) {
        if (format == null) {
            this.format = getOptimalVertexFormat();
        }

        ensureCapacity(this.format.getVertexSize());

        writeVertex((float) (x + xOffset), (float) (y + yOffset), (float) (z + zOffset));
        this.vertexCount++;
    }

    public final void writeVertex(float x, float y, float z) {
        writePtr = format.writeToBuffer0(writePtr, this, x, y, z);
    }

    public final void writeVertex(double x, double y, double z) {
        writeVertex((float) x, (float) y, (float) z);
    }

    @Override
    public void setTextureUV(double u, double v) {
        if (!hasTexture) {
            if (preDefinedFormat != null) return;

            this.hasTexture = true;

            if (format != null) {
                fixBufferFormat();
            }
        }

        writeTextureUV(u, v);
    }

    public final void writeTextureUV(double u, double v) {
        this.textureU = u;
        this.textureV = v;
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

        writeNormal(nx, ny, nz);
    }

    public final void writeNormal(float nx, float ny, float nz) {
        byte b0 = (byte) ((int) (nx * 127.0F));
        byte b1 = (byte) ((int) (ny * 127.0F));
        byte b2 = (byte) ((int) (nz * 127.0F));
        this.normal = (b0 & 255 | (b1 & 255) << 8 | (b2 & 255) << 16);
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

        writeColor(red, green, blue, alpha);
    }

    public final void writeColor(int red, int green, int blue, int alpha) {
        this.color = alpha << 24 | blue << 16 | green << 8 | red;
    }

    public final void writeColor(int color) {
        this.color = color;
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

    /**
     * Uploads the Tessellator to a VBO.
     */
    public final IVertexArrayObject uploadToVBO(VertexBufferType bufferType) {
        if (this.drawMode == GL11.GL_QUADS) {
            return VertexOptimizer.optimizeQuads(bufferType, format, vertexCount, getWriteBuffer());
        }
        return bufferType.allocate(this.format, this.drawMode, getWriteBuffer(), vertexCount);
    }

    /**
     * Uploads the data to the vbo via {@link IVertexBuffer#update}. Requires the data to already be allocated once.
     * <p>
     * May throw {@link UnsupportedOperationException}.
     */
    public final void updateToVBO(IVertexBuffer vbo) {
        vbo.update(getWriteBuffer());
    }

    /**
     * Allocates the data to the vbo via {@link IVertexBuffer#allocate}.
     * <p>
     * Note that, depending on the {@link IVertexBuffer} type, it may not allow further allocations.
     */
    public final void allocateToVBO(IVertexBuffer vbo) {
        vbo.allocate(getWriteBuffer(), this.vertexCount);
    }

    /**
     * Uploads the data to the VBO & uploads the needed indices to the EBO.
     */
    @Beta // May change in the future
    public final void allocateToVBO(IVertexArrayObject vao, IndexBuffer ebo) {
        vao.getVBO().allocate(getWriteBuffer(), this.vertexCount / 4 * 6);
        ebo.upload(vertexCount);
    }

    public ByteBuffer getWriteBuffer() {
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
    public final ByteBuffer allocateBufferCopy() {
        final int size = bufferLimit();
        final ByteBuffer copy = memAlloc(size);
        memCopy(startPtr, memAddress0(copy), size);
        return copy;
    }

    // If some mod does something illegal (like calling setColor after a vertex has been emitted), this will result in
    // undefined behavior, but I still have to take that into account here.
    protected final void fixBufferFormat() {
        final VertexFormat oldFormat = this.format;
        final VertexFormat newFormat = this.getOptimalVertexFormat();

        final int vertexCount = this.vertexCount;
        final int newBufferSize = vertexCount * newFormat.getVertexSize();

        // Allocate temp buffer
        ByteBuffer temp = memAlloc(newBufferSize);
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
        ensureCapacity(newBufferSize); // make sure the main buffer is large enough
        memCopy(tempPtr, startPtr, newBufferSize);
        writePtr = startPtr + newBufferSize;

        memFree(temp); // free temporary buffer
        this.format = newFormat;
    }

    protected final VertexFormat getOptimalVertexFormat() {
        return VertexFlags.getFormat(this);
    }

    @Deprecated // Idk what to do with this, this has caused me too much headache
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

    @Override
    public final TesselatorVertexState getVertexState(float p_147564_1_, float p_147564_2_, float p_147564_3_) {
        throw new UnsupportedOperationException("getVertexState not supported for DirectTessellator!");
    }

    @Override
    public final void setVertexState(TesselatorVertexState p_147565_1_) {
        throw new UnsupportedOperationException("setVertexState not supported for DirectTessellator!");
    }

    protected void onRemovedFromStack() {
        this.preDefinedFormat = null;
        reset();
        if (this.deleteAfter) {
            delete();
        }
    }

    /**
     * Deletes the allocated byte buffer.
     */
    public void delete() {
        reset();
        nmemFree(baseAddress);
    }

    // TessellatorManager delegates

    public static DirectTessellator startCapturing() {
        return TessellatorManager.startCapturingDirect();
    }

    public static DirectTessellator startCapturing(int capacity) {
        return TessellatorManager.startCapturingDirect(capacity);
    }

    public static void startCapturing(DirectTessellator tessellator) {
        TessellatorManager.startCapturingDirect(tessellator);
    }

    public static DirectTessellator startCapturing(VertexFormat format) {
        return TessellatorManager.startCapturingDirect(format);
    }

    @Deprecated
    public static CallbackTessellator startCapturing(DirectDrawCallback callback) {
        return TessellatorManager.startCapturingDirect(callback);
    }

    public static void stopCapturing() {
        TessellatorManager.stopCapturingDirect();
    }

    public static IVertexArrayObject stopCapturingToVBO(VertexBufferType bufferType) {
        return TessellatorManager.stopCapturingDirectToVBO(bufferType);
    }
}
