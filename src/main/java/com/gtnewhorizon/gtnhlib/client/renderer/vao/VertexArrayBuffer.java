package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager.VAO;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VertexArrayBuffer extends VertexBuffer {

    protected final int vaoID;

    /**
     * This constructor is protected in order to prevent the usage of VAO's if they are incompatible. <br>
     * Use VAOManager.createVAO() instead
     */
    protected VertexArrayBuffer(VertexFormat format, int drawMode) {
        super(format, drawMode);
        this.vaoID = VAO.glGenVertexArrays();
        VAO.glBindVertexArray(vaoID);
        bindVBO();
        format.setupBufferState(0L);
        unbindVBO();
        VAO.glBindVertexArray(0);
    }

    @Override
    public void close() {
        super.close();
        if (vaoID >= 0) {
            VAO.glDeleteVertexArrays(vaoID);
        }
    }

    @Override
    public final void render() {
        VAO.glBindVertexArray(vaoID);
        draw();
        VAO.glBindVertexArray(0);
    }

    @Override
    public final void bind() {
        VAO.glBindVertexArray(vaoID);
    }

    @Override
    public final void unbind() {
        VAO.glBindVertexArray(0);
    }

    @Override
    public final void setupState() {
        bind();
    }

    @Override
    public final void cleanupState() {
        unbind();
    }

    public int getVaoID() {
        return vaoID;
    }
}
