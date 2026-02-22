package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VaoFunctions;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VertexBuffer implements IVertexBuffer, AutoCloseable {

    private static final VaoFunctions vao = GLCaps.VAO;

    protected int id;
    protected int vertexCount;
    protected final VertexFormat format;
    protected final int drawMode;
    private int vaoId;
    private boolean vaoDirty;

    public VertexBuffer(VertexFormat format, int drawMode) {
        this.id = GL15.glGenBuffers();
        this.format = format;
        this.drawMode = drawMode;
        this.vaoId = vao != null ? vao.glGenVertexArrays() : -1;
        this.vaoDirty = true;
    }

    public VertexBuffer(VertexFormat format, int drawMode, ByteBuffer buffer, int vertexCount) {
        this(format, drawMode);
        allocate(buffer, vertexCount);
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
        this.vaoDirty = true;
    }

    @Override
    public void allocate(ByteBuffer buffer, int vertexCount) {
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
        if (id > 0) {
            GL15.glDeleteBuffers(this.id);
            id = -1;
        }
        if (vaoId >= 0) {
            vao.glDeleteVertexArrays(vaoId);
            vaoId = -1;
        }
    }

    @Override
    public void setupState() {
        if (vaoId >= 0) {
            vao.glBindVertexArray(vaoId);
            if (vaoDirty) {
                bindVBO();
                format.setupBufferState(0L);
                vaoDirty = false;
            }
        } else {
            bindVBO();
            format.setupBufferState(0L);
        }
    }

    @Override
    public void cleanupState() {
        if (vaoId >= 0) {
            vao.glBindVertexArray(0);
        } else {
            format.clearBufferState();
            unbindVBO();
        }
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
