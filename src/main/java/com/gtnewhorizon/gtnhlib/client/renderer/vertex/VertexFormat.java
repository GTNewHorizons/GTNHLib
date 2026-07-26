package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix4fc;
import org.joml.Vector3f;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import lombok.Getter;

public class VertexFormat {

    @Deprecated
    protected final List<VertexFormatElement> elements;
    public final VertexFormatElement[] elementsArray;

    @Deprecated // Use elementsArray instead
    public List<VertexFormatElement> getElements() {
        return elements;
    }

    @Getter
    public final int vertexSize;
    protected final int[] offsets;

    protected static final List<SetupBufferState> setupBufferStateOverrride = new ArrayList<>();
    protected static final List<ClearBufferState> clearBufferStateOverrride = new ArrayList<>();

    protected final int vertexFlags;

    @Deprecated
    public static void registerSetupBufferStateOverride(SetupBufferState override) {
        setupBufferStateOverrride.add(override);
    }

    @Deprecated
    public static void registerClearBufferStateOverride(ClearBufferState override) {
        clearBufferStateOverrride.add(override);
    }

    public VertexFormat(VertexFormatElement... elements) {
        final int length = elements.length;
        this.offsets = new int[length];

        int offset = 0;
        int flags = 0;
        for (int i = 0; i < length; i++) {
            final VertexFormatElement element = elements[i];

            offsets[i] = offset;
            offset += element.getByteSize();
            flags |= element.vertexBit;
        }
        this.vertexFlags = flags;

        this.elementsArray = elements;
        this.elements = new ObjectImmutableList<>(elements);
        this.vertexSize = offset;
    }

    public void setupBufferState(long l) {
        SetupBufferState override;
        final int overrideSize = setupBufferStateOverrride.size();
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < overrideSize; i++) {
            override = setupBufferStateOverrride.get(i);
            if (override.run(this, l)) {
                return;
            }
        }

        final int i = this.vertexSize;
        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;

        for (int j = 0; j < listSize; ++j) {
            list[j].setupBufferState(l + this.offsets[j], i);
        }
    }

    public void clearBufferState() {
        ClearBufferState override;
        final int overrideSize = clearBufferStateOverrride.size();
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < overrideSize; i++) {
            override = clearBufferStateOverrride.get(i);
            if (override.run(this)) {
                return;
            }
        }
        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;

        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < listSize; ++i) {
            list[i].clearBufferState();
        }
    }

    // Deprecated, will always return true
    @Deprecated
    public boolean canWriteQuads() {
        return true;
    }

    public final void writeQuads(List<ModelQuadViewMutable> quads, ByteBuffer out) {
        final int size = quads.size();
        if (out.remaining() < this.vertexSize * size) throw new IllegalArgumentException(
                "Buffer only has " + out.remaining() + " out of " + this.vertexSize * size + " bytes remaining.");

        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;

        final long address = memAddress0(out);
        long writePointer = address + out.position();

        for (int i = 0; i < size; i++) {
            final ModelQuadView quad = quads.get(i);
            final int[] data = quad.getDataArray();

            for (int index = 0; index < 32; index += 8) {
                for (int j = 0; j < listSize; j++) {
                    writePointer += list[j].writer.writeAttribute(writePointer, data, index);
                }
            }
        }

        out.position((int) (writePointer - address));
    }

    public final void writeQuad(ModelQuadView quad, ByteBuffer out) {
        if (out.remaining() < this.vertexSize) throw new IllegalArgumentException(
                "Buffer only has " + out.remaining() + " out of " + this.vertexSize + " bytes remaining.");

        final int[] data = quad.getDataArray();
        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;

        final long address = memAddress0(out);
        long writePointer = address + out.position();

        for (int index = 0; index < 32; index += 8) {
            for (int i = 0; i < listSize; i++) {
                writePointer += list[i].writer.writeAttribute(writePointer, data, index);
            }
        }
        out.position((int) (writePointer - address));
    }

    public final void writeToBuffer(ByteBuffer out, int[] data, int rawBufferIndex) {
        final int vertexCount = rawBufferIndex >> 3;
        if (out.remaining() < this.vertexSize * vertexCount) throw new IllegalArgumentException(
                "Buffer only has " + out.remaining()
                        + " out of "
                        + this.vertexSize * vertexCount
                        + " bytes remaining.");

        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;

        final long address = memAddress0(out);
        long writePointer = address + out.position();

        for (int index = 0; index < rawBufferIndex; index += 8) {
            for (int j = 0; j < listSize; j++) {
                writePointer += list[j].writer.writeAttribute(writePointer, data, index);
            }
        }

        out.position((int) (writePointer - address));
    }

    public final long writeToBuffer0(long pointer, final int[] data, final int rawBufferIndex) {
        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;

        for (int index = 0; index < rawBufferIndex; index += 8) {
            for (int i = 0; i < listSize; i++) {
                pointer += list[i].writer.writeAttribute(pointer, data, index);
            }
        }
        return pointer;
    }

    public final long writeToBuffer0(long pointer, int[] data, int rawBufferIndex, Matrix4fc transform,
            Vector3f scratch) {
        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;

        for (int index = 0; index < rawBufferIndex; index += 8) {
            for (int i = 0; i < listSize; i++) {
                pointer += list[i].writer.writeAttributeTransformed(pointer, data, index, transform, scratch);
            }
        }
        return pointer;
    }

    public final long writeToBuffer0(long pointer, Tessellator tessellator, float x, float y, float z) {
        // Position
        memPutFloat(pointer, x);
        memPutFloat(pointer + 4, y);
        memPutFloat(pointer + 8, z);
        pointer += 12;

        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;
        for (int i = 1; i < listSize; i++) {
            pointer += list[i].writer.writeAttribute(pointer, tessellator);
        }
        return pointer;
    }

    /**
     * Populate the passed-in Tessellator with the data of the buffer. Position data needs to be read before calling
     * this method.
     */
    public final long readFromBuffer0(long pointer, Tessellator tessellator) {
        final VertexFormatElement[] list = this.elementsArray;
        final int listSize = list.length;
        for (int i = 1; i < listSize; i++) {
            pointer += list[i].writer.readAttribute(pointer, tessellator);
        }
        return pointer;
    }

    public final int getVertexCount(ByteBuffer buffer) {
        return getVertexCount(buffer.limit());
    }

    public final int getVertexCount(int bufferSize) {
        return bufferSize / vertexSize;
    }

    public final int getVertexFlags() {
        return this.vertexFlags;
    }

    public final boolean hasTexture() {
        return (vertexFlags & TEXTURE_BIT) != 0;
    }

    public final boolean hasColor() {
        return (vertexFlags & COLOR_BIT) != 0;
    }

    public final boolean hasNormals() {
        return (vertexFlags & NORMAL_BIT) != 0;
    }

    public final boolean hasBrightness() {
        return (vertexFlags & BRIGHTNESS_BIT) != 0;
    }

    public final VertexFormat getSharedFormat(VertexFormat other) {
        return VertexFlags.getFormat(
                this.hasTexture() || other.hasTexture(),
                this.hasColor() || other.hasColor(),
                this.hasNormals() || other.hasNormals(),
                this.hasBrightness() || other.hasBrightness());
    }

    @Override
    public String toString() {
        return "VertexFormat[size=" + vertexSize
                + " hasTexture="
                + hasTexture()
                + " hasColor="
                + hasColor()
                + " hasNormals="
                + hasNormals()
                + " hasBrightness="
                + hasBrightness()
                + "]";
    }

    @FunctionalInterface
    public interface SetupBufferState {

        boolean run(VertexFormat format, long l);
    }

    @FunctionalInterface
    public interface ClearBufferState {

        boolean run(VertexFormat format);
    }
}
