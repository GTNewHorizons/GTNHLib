package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import lombok.Getter;

public class VertexFormat {

    @Getter
    protected final List<VertexFormatElement> elements;
    @Getter
    protected final int vertexSize;
    protected final int[] offsets;

    protected static final List<SetupBufferState> setupBufferStateOverrride = new ArrayList<>();
    protected static final List<ClearBufferState> clearBufferStateOverrride = new ArrayList<>();

    protected final int vertexFlags;

    public static void registerSetupBufferStateOverride(SetupBufferState override) {
        setupBufferStateOverrride.add(override);
    }

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

        this.elements = new ObjectImmutableList<>(elements);
        this.vertexSize = offset;

        DefaultVertexFormat.ALL_FORMATS[this.vertexFlags] = this;
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

        final int i = this.getVertexSize();
        final List<VertexFormatElement> list = this.getElements();
        final int listSize = list.size();

        for (int j = 0; j < listSize; ++j) {
            list.get(j).setupBufferState(l + this.offsets[j], i);
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
        final List<VertexFormatElement> list = this.getElements();
        final int listSize = list.size();

        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < listSize; ++i) {
            list.get(i).clearBufferState();
        }
    }

    // Deprecated, will always return true
    @Deprecated
    public boolean canWriteQuads() {
        return true;
    }

    public final void writeQuads(List<ModelQuadViewMutable> quads, ByteBuffer out) {
        final int size = quads.size();
        if (out.remaining() < this.getVertexSize() * size) throw new IllegalArgumentException(
                "Buffer only has " + out.remaining() + " out of " + this.getVertexSize() * size + " bytes remaining.");

        final List<VertexFormatElement> list = this.getElements();
        final int listSize = list.size();

        final long address = memAddress0(out);
        long writePointer = address + out.position();

        for (int i = 0; i < size; i++) {
            final ModelQuadView quad = quads.get(i);
            final int[] data = quad.getDataArray();

            for (int index = 0; index < 32; index += 8) {
                for (int j = 0; j < listSize; j++) {
                    writePointer += list.get(j).writer.writeAttribute(writePointer, data, index);
                }
            }
        }

        out.position((int) (writePointer - address));
    }

    public final void writeQuad(ModelQuadView quad, ByteBuffer out) {
        if (out.remaining() < this.getVertexSize()) throw new IllegalArgumentException(
                "Buffer only has " + out.remaining() + " out of " + this.getVertexSize() + " bytes remaining.");

        final int[] data = quad.getDataArray();
        final List<VertexFormatElement> list = this.getElements();
        final int listSize = list.size();

        final long address = memAddress0(out);
        long writePointer = address + out.position();

        for (int index = 0; index < 32; index += 8) {
            for (int i = 0; i < listSize; i++) {
                writePointer += list.get(i).writer.writeAttribute(writePointer, data, index);
            }
        }
        out.position((int) (writePointer - address));
    }

    public final long writeToBuffer0(long pointer, int[] data, int rawVertexCount) {
        final List<VertexFormatElement> list = this.getElements();
        final int listSize = list.size();

        for (int index = 0; index < rawVertexCount; index += 8) {
            for (int i = 0; i < listSize; i++) {
                pointer += list.get(i).writer.writeAttribute(pointer, data, index);
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

        final List<VertexFormatElement> list = this.getElements();
        final int listSize = list.size();
        for (int i = 1; i < listSize; i++) {
            pointer += list.get(i).writer.writeAttribute(pointer, tessellator);
        }
        return pointer;
    }

    /**
     * Populate the passed-in Tessellator with the data of the buffer. Position data needs to be read before calling
     * this method.
     */
    public final long readFromBuffer0(long pointer, Tessellator tessellator) {
        final List<VertexFormatElement> list = this.getElements();
        final int listSize = list.size();
        for (int i = 1; i < listSize; i++) {
            pointer += list.get(i).writer.readAttribute(pointer, tessellator);
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

    @FunctionalInterface
    public interface SetupBufferState {

        boolean run(VertexFormat format, long l);
    }

    @FunctionalInterface
    public interface ClearBufferState {

        boolean run(VertexFormat format);
    }
}
