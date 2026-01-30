package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL44;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IEmptyVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

@Beta
public final class VertexBufferStorage implements IEmptyVertexBuffer {

    private final int id;
    private int vertexCount;
    private final VertexFormat format;
    private final int drawMode;

    public VertexBufferStorage(VertexFormat format, int drawMode) {
        this.id = GL15.glGenBuffers();
        this.format = format;
        this.drawMode = drawMode;
    }

    public VertexBufferStorage(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount, int flags) {
        this(format, drawMode);
        alloc(data, vertexCount, flags);
    }

    @Override
    public void allocate(ByteBuffer buffer, int vertexCount, boolean mutable) {
        alloc(buffer, vertexCount, VAOManager.getStorageFlags(mutable));
    }

    public void alloc(ByteBuffer data, int vertexCount, int flags) {
        this.vertexCount = vertexCount;
        bind();
        GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, data, flags);
        unbind();
    }

    @Override
    public void update(ByteBuffer buffer, long offset) {
        this.bind();
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, buffer);
        this.unbind();
    }

    @Override
    public void bind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.id);
    }

    @Override
    public void unbind() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void delete() {
        GL15.glDeleteBuffers(this.id);
    }

    @Override
    public void setupState() {
        bind();
        format.setupBufferState(0L);
    }

    @Override
    public void cleanupState() {
        format.clearBufferState();
        unbind();
    }

    @Override
    public void draw() {
        GL11.glDrawArrays(this.drawMode, 0, this.vertexCount);
    }

    @Override
    public void draw(int first, int count) {
        GL11.glDrawArrays(this.drawMode, first, count);
    }

    @Override
    public void draw(int drawMode, int first, int count) {
        GL11.glDrawArrays(drawMode, first, count);
    }

    @Override
    public VertexFormat getVertexFormat() {
        return format;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getDrawMode() {
        return drawMode;
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }
}
