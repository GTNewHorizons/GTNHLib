package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import io.netty.buffer.ByteBuf;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public interface IVertexBuffer {

    void allocate(ByteBuffer buffer, int vertexCount, boolean mutable);

    void update(ByteBuffer buffer, long offset);

    default void update(ByteBuffer buffer) {
        update(buffer, 0);
    }

    void bind();

    void unbind();

    void setupState();

    void cleanupState();

    void draw();

    /**
     * Draw a range of vertices from this buffer.
     *
     * @param first First vertex index to draw
     * @param count Number of vertices to draw
     */
    void draw(int first, int count);

    void delete();

    int getId();

    VertexFormat getVertexFormat();

    default void allocate(ByteBuffer buffer, boolean mutable) {
        allocate(buffer, getVertexFormat().getVertexCount(buffer), mutable);
    }

    default void render() {
        setupState();
        draw();
        cleanupState();
    }

    default void draw(FloatBuffer floatBuffer) {
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMultMatrix(floatBuffer);
        draw();
        GL11.glPopMatrix();
    }

    default void render(int drawMode, int first, int count) {
        setupState();
        draw(drawMode, first, count);
        cleanupState();
    }

    static void draw(int drawMode, int first, int count) {
        GL11.glDrawArrays(drawMode, first, count);
    }

    default void render(int first, int count) {
        setupState();
        draw(first, count);
        cleanupState();
    }

    default void render(FloatBuffer floatBuffer) {
        setupState();
        draw(floatBuffer);
        cleanupState();
    }

}
