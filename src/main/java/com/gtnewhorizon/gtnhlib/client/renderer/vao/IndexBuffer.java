package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL44;

import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;

public final class IndexBuffer {

    static final int EBO_DATA_TYPE = GL11.GL_UNSIGNED_SHORT;
    static final int EBO_DATA_SIZE = 2; // short
    private int id;

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
        final ByteBuffer data = createQuadEBOBuffer(start, end);
        upload(data);
        memFree(data);
    }

    public int getId() {
        return this.id;
    }

    /**
     * Allocates a buffer that contains the needed indices to map GL_QUADS into GL_TRIANGLES.
     * <p>
     * Buffer must be freed via {@link com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities#memFree(ByteBuffer)} afterwards.
     */
    private static ByteBuffer createQuadEBOBuffer(int vertexCount) {
        return createQuadEBOBuffer(0, vertexCount);
    }

    /**
     * Allocates a buffer that contains the needed indices to map GL_QUADS into GL_TRIANGLES.
     * <p>
     * Buffer must be freed via {@link com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities#memFree(ByteBuffer)} afterwards.
     */
    private static ByteBuffer createQuadEBOBuffer(int start, int end) {
        final int quadCount = (end - start) / 4;
        final ByteBuffer data = memAlloc(quadCount * 6 * 2);
        long ptr = memAddress0(data);
        for (int i = 0; i < quadCount; i++) {
            int base = (start + i * 4);

            // triangle 1
            memPutShort(ptr, (short) base);
            memPutShort(ptr + 2, (short) (base + 1));
            memPutShort(ptr + 4, (short) (base + 2));

            // triangle 2
            memPutShort(ptr + 6, (short) (base + 2));
            memPutShort(ptr + 8, (short) (base + 3));
            memPutShort(ptr + 10, (short) base);
            ptr += 12;
        }
        return data;
    }

    public static IndexBuffer convertQuadsToTrigs(int vertexCount) {
        return convertQuadsToTrigs(0, vertexCount);
    }

    public static IndexBuffer convertQuadsToTrigs(int start, int end) {
        final IndexBuffer ebo = new IndexBuffer();

        final ByteBuffer data = createQuadEBOBuffer(start, end);
        ebo.allocateImmutable(data);

        memFree(data);

        return ebo;
    }
}
