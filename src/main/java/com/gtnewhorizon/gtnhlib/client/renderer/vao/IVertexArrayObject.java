package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;

public interface IVertexArrayObject {

    void bind();

    void unbind();

    void draw();

    void draw(int first, int count);

    void draw(int drawMode, int first, int count);

    void delete();

    IVertexBuffer getVBO();

    default void render() {
        bind();
        draw();
        unbind();
    }

    default void render(int first, int count) {
        bind();
        draw(first, count);
        unbind();
    }
}
