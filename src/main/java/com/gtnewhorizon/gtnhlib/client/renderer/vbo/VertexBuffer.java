package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VertexBuffer implements IVertexBuffer, AutoCloseable {

    protected int id;
    protected int vertexCount;
    protected final VertexFormat format;
    protected final int drawMode;

    public VertexBuffer(VertexFormat format, int drawMode) {
        this.id = GL15.glGenBuffers();
        this.format = format;
        this.drawMode = drawMode;
    }

    public VertexBuffer(VertexFormat format, int drawMode, ByteBuffer buffer, int vertexCount) {
        this.id = GL15.glGenBuffers();
        this.format = format;
        this.drawMode = drawMode;
        this.vertexCount = vertexCount;
        this.bindVBO();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        this.unbindVBO();
    }

    public VertexBuffer(VertexFormat format) {
        this(format, GL11.GL_QUADS);
    }

    @Override
    public void bind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.id);
    }

    @Override
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

    public void upload(ByteBuffer buffer, int vertexCount, int type) {
        this.vertexCount = vertexCount;
        this.bindVBO();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, type);
        this.unbindVBO();
    }

    @Override
    public void allocate(ByteBuffer buffer, int vertexCount, int flags) {
        upload(buffer, vertexCount, GL15.GL_STATIC_DRAW);
    }

    @Override
    public void update(ByteBuffer buffer, long offset) {
        this.bindVBO();
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, buffer);
        this.unbindVBO();
    }

    public VertexBuffer upload(ByteBuffer buffer) {
        upload(buffer, format.getVertexCount(buffer), GL15.GL_STATIC_DRAW);
        return this;
    }

    public void upload(ByteBuffer buffer, int vertexCount) {
        upload(buffer, vertexCount, GL15.GL_STATIC_DRAW);
    }

    /**
     * GL_DYNAMIC_DRAW is more efficient for buffers that have their values constantly updated and read multiple times.
     */
    public void uploadDynamic(ByteBuffer buffer, int vertexCount) {
        upload(buffer, vertexCount, GL15.GL_DYNAMIC_DRAW);
    }

    public void uploadDynamic(ByteBuffer buffer) {
        upload(buffer, format.getVertexCount(buffer), GL15.GL_DYNAMIC_DRAW);
    }

    /**
     * GL_STREAM_DRAW is more efficient for buffers that have their values constantly updated and read at most a few
     * times (example: uploading a set of vertices that only get used a few times at most)
     */
    public void uploadStream(ByteBuffer buffer, int vertexCount) {
        upload(buffer, vertexCount, GL15.GL_STREAM_DRAW);
    }

    public void uploadStream(ByteBuffer buffer) {
        upload(buffer, format.getVertexCount(buffer), GL15.GL_STREAM_DRAW);
    }

    @Deprecated // For clarity, use delete() instead
    public final void close() {
        this.delete();
    }

    @Override
    public void delete() {
        GL15.glDeleteBuffers(this.id);
    }

    @Override
    public void setupState() {
        bindVBO();
        format.setupBufferState(0L);
    }

    @Override
    public void cleanupState() {
        format.clearBufferState();
        unbindVBO();
    }

    @Override
    public final void draw() {
        GL11.glDrawArrays(this.drawMode, 0, this.vertexCount);
    }

    @Override
    public final void draw(int first, int count) {
        GL11.glDrawArrays(this.drawMode, first, count);
    }

    @Override
    public final void draw(int drawMode, int first, int count) {
        GL11.glDrawArrays(drawMode, first, count);
    }

    @Override
    public final VertexFormat getVertexFormat() {
        return format;
    }

    @Override
    public final int getId() {
        return id;
    }

    @Override
    public final int getDrawMode() {
        return drawMode;
    }

    @Override
    public final int getVertexCount() {
        return vertexCount;
    }
}
