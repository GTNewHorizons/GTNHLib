package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public interface VertexBufferFactory {

    IVertexArrayObject allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount);

    IVertexArrayObject allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount, IndexBuffer ebo);
}
