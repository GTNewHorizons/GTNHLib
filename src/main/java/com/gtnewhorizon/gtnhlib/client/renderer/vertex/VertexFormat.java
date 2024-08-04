package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.writers.IWriteQuads;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

public class VertexFormat {

    @Getter
    protected final ImmutableList<VertexFormatElement> elements;
    @Getter
    protected final int vertexSize;
    protected final IntList offsets = new IntArrayList();
    protected IWriteQuads quadWriter;

    protected static final List<SetupBufferState> setupBufferStateOverrride = new ArrayList<>();
    protected static final List<ClearBufferState> clearBufferStateOverrride = new ArrayList<>();

    public static void registerSetupBufferStateOverride(SetupBufferState override) {
        setupBufferStateOverrride.add(override);
    }

    public static void registerClearBufferStateOverride(ClearBufferState override) {
        clearBufferStateOverrride.add(override);
    }

    public VertexFormat(ImmutableList<VertexFormatElement> elements) {
        this(elements, null);
    }

    public VertexFormat(ImmutableList<VertexFormatElement> elements, IWriteQuads quadWriter) {
        this.elements = elements;
        int i = 0;
        for (VertexFormatElement element : elements) {
            offsets.add(i);
            i += element.getByteSize();
        }
        vertexSize = i;
        this.quadWriter = quadWriter;
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
            list.get(j).setupBufferState(l + this.offsets.getInt(j), i);
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

    public boolean canWriteQuads() {
        return quadWriter != null;
    }

    public void writeQuad(QuadView quad, ByteBuffer byteBuffer) {
        if (quadWriter == null) {
            throw new IllegalStateException("No quad writer set");
        }
        quadWriter.writeQuad(quad, byteBuffer);
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
