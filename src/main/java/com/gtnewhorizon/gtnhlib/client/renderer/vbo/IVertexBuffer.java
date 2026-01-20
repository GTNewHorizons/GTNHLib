package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

/**
 * Represents a GPU-side vertex buffer object (VBO).
 * <p>
 * Any implementation that returns a {@link IVertexBuffer} is already allocated and ready to use. Depending on the
 * implementation, it may not allow further {@link #update} calls (see more under
 * {@link com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType}
 * </p>
 */
public interface IVertexBuffer {

    /**
     * Binds this vertex buffer to the current OpenGL {@code GL_ARRAY_BUFFER} target.
     */
    void bind();

    /**
     * Unbinds any vertex buffer from the {@code GL_ARRAY_BUFFER} target.
     */
    void unbind();

    /**
     * Updates a subrange of this vertex buffer's contents.
     * <p>
     * Note that some implementations may not allow any mutations on the contents (see more under
     * {@link com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType}
     * </p>
     *
     * @param buffer the data to write into the buffer
     * @param offset byte offset into the buffer at which to start writing
     */
    void update(ByteBuffer buffer, long offset);

    /**
     * Configures the vertex attribute state required to interpret this buffer's data.
     * <p>
     * This method typically enables and specifies vertex attribute pointers according to the associated
     * {@link VertexFormat}.
     * </p>
     *
     * <p>
     * The buffer will be bound as part of this call.
     * </p>
     */
    void setupState();

    /**
     * Resets or disables the vertex attribute state configured by {@link #setupState()}.
     * <p>
     * Implementations should restore OpenGL state such that subsequent draw calls are not affected by this buffer's
     * attribute configuration.
     * </p>
     */
    void cleanupState();

    /**
     * Issues a draw call using the entire contents of this vertex buffer.
     * <p>
     * The draw mode and vertex count are implementation-defined.
     * </p>
     */
    void draw();

    /**
     * Issues a draw call using a subset of this vertex buffer.
     *
     * @param first index of the first vertex to draw
     * @param count number of vertices to draw
     */
    void draw(int first, int count);

    /**
     * Deletes the underlying GPU buffer and releases any associated resources.
     * <p>
     * After calling this method, the buffer is no longer valid and must not be used.
     * </p>
     */
    void delete();

    /**
     * Returns the vertex format describing the layout of the data stored in this buffer.
     *
     * @return the vertex format
     */
    VertexFormat getVertexFormat();

    /**
     * Returns the OpenGL object ID of the underlying vertex buffer.
     *
     * @return the OpenGL buffer ID, or a negative value if the buffer has been deleted
     */
    int getId();

    default void update(ByteBuffer buffer) {
        update(buffer, 0);
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
