package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IndexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;

public final class VertexOptimizer {

    public static IVertexArrayObject optimizeQuads(VertexBufferType type, VertexFormat format, int vertexCount,
            ByteBuffer data) {
        if (vertexCount == 4) {
            convertQuadsToTriStrip(format, data);
            return type.allocate(format, GL11.GL_TRIANGLE_STRIP, data, 4);
        }

        return type.allocate(
                format,
                GL11.GL_TRIANGLES,
                data,
                vertexCount / 4 * 6,
                IndexBuffer.convertQuadsToTrigs(vertexCount));
    }

    public static void optimizeQuads(VertexFormat format, int vertexCount, ByteBuffer data) {
        if (vertexCount == 4) {
            convertQuadsToTriStrip(format, data);
        }
    }

    // Convert GL_QUADS to GL_TRIANGLE_STRIP
    // Converts (v0, v1, v2, v3) -> (v0, v1, v3, v2)
    public static void convertQuadsToTriStrip(VertexFormat format, ByteBuffer data) {
        final int vertexSize = format.vertexSize;
        final long base = memAddress0(data);
        final long v2 = base + 2L * vertexSize;
        final long v3 = base + 3L * vertexSize;

        long tmp = nmemAllocChecked(vertexSize);
        // v2 → tmp
        memCopy(v2, tmp, vertexSize);
        // v3 → v2
        memCopy(v3, v2, vertexSize);
        // tmp → v3
        memCopy(tmp, v3, vertexSize);
        // free if not reused
        nmemFree(tmp);
    }
}
