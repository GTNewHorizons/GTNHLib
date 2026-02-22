package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL44;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

@Beta
public final class VertexBufferStorage implements IVertexBuffer {

    private static final VaoFunctions vao = GLCaps.VAO;

    private int id;
    private int vertexCount;
    private final VertexFormat format;
    private final int drawMode;
    private final int flags;
    private int vaoId;
    private boolean vaoDirty;

    public VertexBufferStorage(VertexFormat format, int drawMode, int flags) {
        this.id = GL15.glGenBuffers();
        this.format = format;
        this.drawMode = drawMode;
        this.flags = flags;
        this.vaoId = vao != null ? vao.glGenVertexArrays() : -1;
        this.vaoDirty = true;
    }

    public VertexBufferStorage(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount, int flags) {
        this(format, drawMode, flags);
        allocate(data, vertexCount);
    }

    @Override
    public void allocate(ByteBuffer data, int vertexCount) {
        this.vertexCount = vertexCount;
        bind();
        GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, data, this.flags);
        unbind();
        this.vaoDirty = true;
    }

    @Override
    public void update(ByteBuffer buffer, long offset) {
        if ((this.flags & GL44.GL_DYNAMIC_STORAGE_BIT) == 0) {
            throw new UnsupportedOperationException("Cannot call update() on an immutable buffer!");
        }
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
                bind();
                format.setupBufferState(0L);
                vaoDirty = false;
            }
        } else {
            bind();
            format.setupBufferState(0L);
        }
    }

    @Override
    public void cleanupState() {
        if (vaoId >= 0) {
            vao.glBindVertexArray(0);
        } else {
            format.clearBufferState();
            unbind();
        }
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
