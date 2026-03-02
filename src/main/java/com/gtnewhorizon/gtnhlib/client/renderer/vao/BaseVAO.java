package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager.VAO;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;

public class BaseVAO implements IVertexArrayObject {

    protected int vaoID = -1;

    protected final IVertexBuffer vbo;

    public BaseVAO(IVertexBuffer vbo) {
        this.vbo = vbo;
    }

    @Override
    public final void bind() {
        if (isInitialized()) {
            VAO.glBindVertexArray(vaoID);
            return;
        }
        this.vaoID = VAO.glGenVertexArrays();
        VAO.glBindVertexArray(vaoID);
        setupVAOStates();
    }

    @Override
    public final void unbind() {
        VAO.glBindVertexArray(0);
    }

    @Override
    public void draw() {
        vbo.draw();
    }

    @Override
    public void draw(int first, int count) {
        vbo.draw(first, count);
    }

    @Override
    public void draw(int drawMode, int first, int count) {
        vbo.draw(drawMode, first, count);
    }

    @Override
    public void delete() {
        vbo.delete();
        if (vaoID >= 0) {
            VAO.glDeleteVertexArrays(vaoID);
            vaoID = -1;
        }
    }

    @Override
    public final IVertexBuffer getVBO() {
        return this.vbo;
    }

    protected void setupVAOStates() {
        vbo.setupState();
        vbo.unbind();
    }

    protected final boolean isInitialized() {
        return vaoID != -1;
    }
}
