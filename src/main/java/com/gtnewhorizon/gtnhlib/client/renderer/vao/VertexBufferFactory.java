package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public interface VertexBufferFactory {

    IVertexBuffer allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount);

    // IVertexBuffer create(VertexFormat format, int drawMode);
}
