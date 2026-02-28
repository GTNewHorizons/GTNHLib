package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import java.util.function.IntConsumer;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers.IVertexAttributeWriter;

import lombok.Getter;

@Getter
public class VertexFormatElement {

    protected final Type type;
    protected final Usage usage;
    protected final int index;
    protected final int count;
    protected final int byteSize;
    @Beta
    protected final @NotNull IVertexAttributeWriter writer;
    protected final int vertexBit;

    public VertexFormatElement(int index, Type type, Usage usage, int count, int vertexBit,
            @NotNull IVertexAttributeWriter writer) {
        this(index, type, usage, count, vertexBit, writer, 0);
    }

    public VertexFormatElement(int index, Type type, Usage usage, int count, int vertexBit,
            @NotNull IVertexAttributeWriter writer, int padding) {
        this.index = index;
        this.type = type;
        this.usage = usage;
        this.count = count;
        this.byteSize = type.getSize() * count + padding;
        this.vertexBit = vertexBit;
        this.writer = writer;
    }

    public void setupBufferState(long offset, int stride) {
        this.usage.setupBufferState(this.count, this.type.getGlType(), stride, offset, this.index);
    }

    public void clearBufferState() {
        this.usage.clearBufferState(this.index);
    }

    public enum Usage {

        POSITION("Position", 0, false, (size, type, stride, pointer, index) -> {
            GL11.glVertexPointer(size, type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        }, index -> GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY)),
        NORMAL("Normal", 4, true, (size, type, stride, pointer, index) -> {
            GL11.glNormalPointer(type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        }, index -> GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY)),
        COLOR("Vertex Color", 1, true, (size, type, stride, pointer, index) -> {
            GL11.glColorPointer(size, type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        }, index -> GL11.glDisableClientState(GL11.GL_COLOR_ARRAY)),
        PRIMARY_UV("UV 0", 2, false, (size, type, stride, pointer, index) -> {
            GL11.glTexCoordPointer(size, type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        }, index -> GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY)),
        SECONDARY_UV("UV 1-31", 3, false, (size, type, stride, pointer, index) -> {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + index);
            GL11.glTexCoordPointer(size, type, stride, pointer);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
        }, index -> {
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + index);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
        }),
        PADDING("Padding", -1, false, (size, type, stride, pointer, index) -> {}, index -> {}),
        GENERIC("Generic", -1, false, (size, type, stride, pointer, index) -> {
            GL20.glEnableVertexAttribArray(index);
            GL20.glVertexAttribPointer(index, size, type, false, stride, pointer);
        }, GL20::glDisableVertexAttribArray);

        @Getter
        private final String name;
        @Getter
        private final int attributeLocation;
        @Getter
        private final boolean normalized;
        private final SetupState setupState;
        private final IntConsumer clearState;

        Usage(String name, int attributeLocation, boolean normalized, SetupState setupState, IntConsumer clearState) {
            this.name = name;
            this.attributeLocation = attributeLocation;
            this.normalized = normalized;
            this.setupState = setupState;
            this.clearState = clearState;
        }

        private void setupBufferState(int size, int type, int stride, long pointer, int index) {
            if (GLCaps.isCoreProfile()) {
                if (this == PADDING) return;
                final int location = attributeLocation >= 0 ? attributeLocation : index;
                GL20.glVertexAttribPointer(location, size, type, normalized, stride, pointer);
                GL20.glEnableVertexAttribArray(location);
            } else {
                this.setupState.setupBufferState(size, type, stride, pointer, index);
            }
        }

        public void clearBufferState(int index) {
            if (GLCaps.isCoreProfile()) {
                if (this == PADDING) return;
                final int location = attributeLocation >= 0 ? attributeLocation : index;
                GL20.glDisableVertexAttribArray(location);
            } else {
                this.clearState.accept(index);
            }
        }

        interface SetupState {

            void setupBufferState(int size, int type, int stride, long pointer, int index);
        }
    }

    @Getter
    public enum Type {

        FLOAT(4, "Float", GL11.GL_FLOAT),
        UBYTE(1, "Unsigned Byte", GL11.GL_UNSIGNED_BYTE),
        BYTE(1, "Byte", GL11.GL_BYTE),
        USHORT(2, "Unsigned Short", GL11.GL_UNSIGNED_SHORT),
        SHORT(2, "Short", GL11.GL_SHORT),
        UINT(4, "Unsigned Int", GL11.GL_UNSIGNED_INT),
        INT(4, "Int", GL11.GL_INT);

        private final int size;
        private final String name;
        private final int glType;

        Type(int size, String name, int glType) {
            this.size = size;
            this.name = name;
            this.glType = glType;
        }
    }
}
