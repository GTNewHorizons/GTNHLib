package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL44;

import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;

public final class IndexBuffer {

    private int id;
    private int dataType = GL11.GL_UNSIGNED_SHORT;
    private int dataSize = 2;

    public IndexBuffer() {
        id = GL15.glGenBuffers();
    }

    public void bind() {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, id);
    }

    public void unbind() {
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void delete() {
        if (id > 0) {
            GL15.glDeleteBuffers(id);
            id = -1;
        }
    }

    public void allocateImmutable(ByteBuffer data) {
        if (GLCaps.bufferStorageSupported()) {
            bind();
            GL44.glBufferStorage(GL15.GL_ELEMENT_ARRAY_BUFFER, data, 0);
            unbind();
            return;
        }

        upload(data);
    }

    public void upload(ByteBuffer data) {
        bind();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
        unbind();
    }

    public void upload(int vertexCount) {
        upload(0, vertexCount);
    }

    public void upload(int start, int end) {
        setIndexType(selectIndexType(end));
        final ByteBuffer data = createQuadEBOBuffer(start, end, dataType);
        upload(data);
        memFree(data);
    }

    public int getId() {
        return this.id;
    }

    public int getDataType() {
        return this.dataType;
    }

    public int getDataSize() {
        return this.dataSize;
    }

    /**
     * Allocates a buffer that contains the needed indices to map GL_QUADS into GL_TRIANGLES.
     * <p>
     * Buffer must be freed via {@link com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities#memFree(ByteBuffer)} afterwards.
     */
    private static ByteBuffer createQuadEBOBuffer(int vertexCount) {
        return createQuadEBOBuffer(0, vertexCount, selectIndexType(vertexCount));
    }

    /**
     * Allocates a buffer that contains the needed indices to map GL_QUADS into GL_TRIANGLES.
     * <p>
     * Buffer must be freed via {@link com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities#memFree(ByteBuffer)} afterwards.
     */
    private static ByteBuffer createQuadEBOBuffer(int start, int end, int indexType) {
        final int quadCount = (end - start) / 4;
        final int indexSize = getIndexSize(indexType);
        final ByteBuffer data = memAlloc(quadCount * 6 * indexSize);
        long ptr = memAddress0(data);
        for (int i = 0; i < quadCount; i++) {
            int base = (start + i * 4);

            // triangle 1
            putIndex(ptr, indexType, base);
            putIndex(ptr + indexSize, indexType, base + 1);
            putIndex(ptr + indexSize * 2L, indexType, base + 2);

            // triangle 2
            putIndex(ptr + indexSize * 3L, indexType, base + 2);
            putIndex(ptr + indexSize * 4L, indexType, base + 3);
            putIndex(ptr + indexSize * 5L, indexType, base);
            ptr += indexSize * 6L;
        }
        return data;
    }

    public static IndexBuffer convertQuadsToTrigs(int vertexCount) {
        return convertQuadsToTrigs(0, vertexCount);
    }

    public static IndexBuffer convertQuadsToTrigs(int start, int end) {
        final IndexBuffer ebo = new IndexBuffer();
        ebo.setIndexType(selectIndexType(end));

        final ByteBuffer data = createQuadEBOBuffer(start, end, ebo.dataType);
        ebo.allocateImmutable(data);

        memFree(data);

        return ebo;
    }

    private void setIndexType(int indexType) {
        this.dataType = indexType;
        this.dataSize = getIndexSize(indexType);
    }

    private static int selectIndexType(int endExclusive) {
        return endExclusive <= 0x10000 ? GL11.GL_UNSIGNED_SHORT : GL11.GL_UNSIGNED_INT;
    }

    private static int getIndexSize(int indexType) {
        return switch (indexType) {
            case GL11.GL_UNSIGNED_SHORT -> 2;
            case GL11.GL_UNSIGNED_INT -> 4;
            default -> throw new IllegalArgumentException("Unsupported index type: " + indexType);
        };
    }

    private static void putIndex(long ptr, int indexType, int value) {
        switch (indexType) {
            case GL11.GL_UNSIGNED_SHORT -> memPutShort(ptr, (short) value);
            case GL11.GL_UNSIGNED_INT -> memPutInt(ptr, value);
            default -> throw new IllegalArgumentException("Unsupported index type: " + indexType);
        }
    }
}
