package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;

public class VertexArrayUnsupported implements IVertexArrayObject {

    protected final IVertexBuffer vbo;

    public VertexArrayUnsupported(IVertexBuffer vbo) {
        this.vbo = vbo;
    }

    @Override
    public final void bind() {
        setupVAOStates();
    }

    @Override
    public final void unbind() {
        cleanupVAOStates();
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
    }

    @Override
    public final IVertexBuffer getVBO() {
        return this.vbo;
    }

    protected void setupVAOStates() {
        vbo.setupState();
    }

    protected void cleanupVAOStates() {
        vbo.cleanupState();
    }
}
