package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VertexBuffer implements AutoCloseable {

    protected int id;
    protected int vertexCount;
    protected final VertexFormat format;
    protected final int drawMode;

    public VertexBuffer(VertexFormat format, int drawMode) {
        if (format == null) throw new IllegalStateException("No format specified for VBO");
        this.id = GL15.glGenBuffers();
        this.format = format;
        this.drawMode = drawMode;
    }

    public void bind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.id);
    }

    public void unbind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    // Same as the methods above, but these are safer to use internally (due to VertexArrayBuffer override)
    protected final void bindVBO() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.id);
    }

    protected final void unbindVBO() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void upload(ByteBuffer buffer, int vertexCount) {
        upload(buffer, vertexCount, GL15.GL_STATIC_DRAW);
    }

    public void upload(ByteBuffer buffer, int vertexCount, int type) {
        if (this.id == -1) return;
        this.vertexCount = vertexCount;
        this.bindVBO();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, type);
        this.unbindVBO();
    }

    public VertexBuffer upload(ByteBuffer buffer) {
        upload(buffer, buffer.remaining() / format.getVertexSize(), GL15.GL_STATIC_DRAW);
        return this;
    }

    /**
     * GL_DYNAMIC_DRAW is more efficient for buffers that have their values constantly updated and read multiple times.
     */
    public void uploadDynamic(ByteBuffer buffer, int vertexCount) {
        upload(buffer, vertexCount, GL15.GL_DYNAMIC_DRAW);
    }

    public void uploadDynamic(ByteBuffer buffer) {
        upload(buffer, buffer.remaining() / format.getVertexSize(), GL15.GL_DYNAMIC_DRAW);
    }

    /**
     * GL_STREAM_DRAW is more efficient for buffers that have their values constantly updated and read at most a few
     * times (example: uploading a set of vertices that only get used a few times at most)
     */
    public void uploadStream(ByteBuffer buffer, int vertexCount) {
        upload(buffer, vertexCount, GL15.GL_STREAM_DRAW);
    }

    public void uploadStream(ByteBuffer buffer) {
        upload(buffer, buffer.remaining() / format.getVertexSize(), GL15.GL_STREAM_DRAW);
    }

    public void close() {
        if (this.id >= 0) {
            GL15.glDeleteBuffers(this.id);
            this.id = -1;
        }
    }

    public void draw(FloatBuffer floatBuffer) {
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glMultMatrix(floatBuffer);
        draw();
        GL11.glPopMatrix();
    }

    public final void draw() {
        GL11.glDrawArrays(drawMode, 0, this.vertexCount);
    }

    public void setupState() {
        bindVBO();
        format.setupBufferState(0L);
    }

    public void cleanupState() {
        format.clearBufferState();
        unbindVBO();
    }

    public void render() {
        setupState();
        draw();
        cleanupState();
    }

    public VertexFormat getVertexFormat() {
        return format;
    }

    public int getDrawMode() {
        return drawMode;
    }

    public int getVertexCount() {
        return vertexCount;
    }
}
