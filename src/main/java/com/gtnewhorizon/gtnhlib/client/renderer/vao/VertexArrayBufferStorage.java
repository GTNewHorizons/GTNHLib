package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager.VAO;

import java.nio.ByteBuffer;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

@Beta
public final class VertexArrayBufferStorage extends VertexBufferStorage {

    private int vaoID = -1;

    VertexArrayBufferStorage(VertexFormat format, int drawMode) {
        super(format, drawMode);
    }

    VertexArrayBufferStorage(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount, int flags) {
        super(format, drawMode, data, vertexCount, flags);
    }

    @Override
    public void delete() {
        super.delete();
        if (vaoID != -1) {
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
