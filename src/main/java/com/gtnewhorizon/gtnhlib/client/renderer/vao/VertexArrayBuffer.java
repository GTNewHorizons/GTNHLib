package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager.VAO;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

@Deprecated
public final class VertexArrayBuffer extends VertexBuffer {

    private int vaoID = -1;

    /**
     * This constructor is protected in order to prevent the usage of VAO's if they are incompatible. <br>
     * Use VAOManager.createVAO() instead
     */
    VertexArrayBuffer(VertexFormat format, int drawMode) {
        super(format, drawMode);
    }

    VertexArrayBuffer(VertexFormat format) {
        super(format);
    }

    VertexArrayBuffer(VertexFormat format, int drawMode, ByteBuffer buffer, int vertexCount) {
        super(format, drawMode, buffer, vertexCount);
    }

    @Override
    public void delete() {
        super.delete();
        if (vaoID >= 0) {
            VAO.glDeleteVertexArrays(vaoID);
            vaoID = -1;
        }
    }

    @Override
    public void bind() {
        if (vaoID == -1) {
            this.vaoID = VAO.glGenVertexArrays();
            VAO.glBindVertexArray(vaoID);
            bindVBO();
            format.setupBufferState(0);
            unbindVBO();
        } else {
            VAO.glBindVertexArray(vaoID);
        }
    }

    @Override
    public void unbind() {
        VAO.glBindVertexArray(0);
    }

    @Override
    public void setupState() {
        bind();
    }

    @Override
    public void cleanupState() {
        unbind();
    }

    public int getVaoID() {
        return vaoID;
    }
}
