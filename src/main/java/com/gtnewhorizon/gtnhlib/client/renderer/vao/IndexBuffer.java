package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL44;

import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;

public final class IndexBuffer {

    static final int EBO_DATA_SIZE = GL11.GL_UNSIGNED_SHORT;
    private final int id;

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
        GL15.glDeleteBuffers(id);
    }

    public void allocateImmutable(ByteBuffer data) {
        bind();
        if (GLCaps.bufferStorageSupported()) {
            GL44.glBufferStorage(GL15.GL_ELEMENT_ARRAY_BUFFER, data, 0);
        } else {
            upload(data);
        }
        unbind();
    }

    public void upload(ByteBuffer data) {
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
    }

    public int getId() {
        return this.id;
    }

    public static IndexBuffer convertQuadsToTrigs(int vertexCount) {
        return convertQuadsToTrigs(0, vertexCount);
    }

    public static IndexBuffer convertQuadsToTrigs(int start, int end) {
        IndexBuffer ebo = new IndexBuffer();

        final int quadCount = (end - start) / 4;
        ByteBuffer data = memAlloc(quadCount * 6 * 2);
        long address = memAddress0(data);
        long ptr = address;
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

        ebo.allocateImmutable(data);

        nmemFree(address);

        return ebo;
    }
}
