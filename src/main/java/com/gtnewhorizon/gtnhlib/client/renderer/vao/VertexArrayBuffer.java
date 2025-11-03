package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager.VAO;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL15;

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
        this.vaoID = UniversalVAO.genVertexArrays();
        VAO.glBindVertexArray(vaoID);
        super.bind();
        format.setupBufferState(0L);
        super.unbind();
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
    public final void upload(ByteBuffer buffer, int vertexCount, int type) {
        if (this.id == -1) return;
        this.vertexCount = vertexCount;
        super.bind();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, type);
        super.unbind();
    }

    @Override
    public final void bind() {
        UniversalVAO.bindVertexArray(vaoID);
    }

    @Override
    public final void unbind() {
        UniversalVAO.bindVertexArray(0);
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
