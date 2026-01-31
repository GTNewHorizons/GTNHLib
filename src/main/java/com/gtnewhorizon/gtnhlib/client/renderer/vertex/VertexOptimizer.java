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

        return type.allocate(
                format,
                GL11.GL_TRIANGLES,
                data,
                vertexCount / 4 * 6,
                IndexBuffer.convertQuadsToTrigs(vertexCount));
    }
}
