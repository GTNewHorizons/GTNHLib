package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager.VAO;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VertexArrayBuffer extends VertexBuffer {

    protected int vaoID = -1;

    /**
     * This constructor is protected in order to prevent the usage of VAO's if they are incompatible. <br>
     * Use VAOManager.createVAO() instead
     */
    protected VertexArrayBuffer(VertexFormat format, int drawMode) {
        super(format, drawMode);
    }

    @Override
    public void close() {
        super.close();
        if (vaoID >= 0) {
            VAO.glDeleteVertexArrays(vaoID);
            vaoID = -1;
        }
    }

    @Override
    public final void bind() {
        if (vaoID == -1) {
            this.vaoID = VAO.glGenVertexArrays();
            VAO.glBindVertexArray(vaoID);
            bindVBO();
            format.setupBufferStateUnsafe();
            unbindVBO();
        } else {
            VAO.glBindVertexArray(vaoID);
        }
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
