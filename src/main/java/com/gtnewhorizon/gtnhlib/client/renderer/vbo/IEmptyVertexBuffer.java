package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import java.nio.ByteBuffer;

/**
 * A vertex buffer object (VBO) that is not yet allocated.
 */
public interface IEmptyVertexBuffer extends IVertexBuffer {

    /**
     * Allocates or reallocates storage for this vertex buffer and uploads initial data.
     * <p>
     * This call defines the size of the buffer and replaces any existing contents. Any previous storage associated with
     * this buffer is discarded.
     * </p>
     * <p>
     * Note that some implementations may not allow more than 1 allocate() call per VertexBuffer (see more under
     * {@link com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType}
     * </p>
     *
     * @param buffer      the vertex data to upload; its remaining bytes determine the buffer size
     * @param vertexCount the number of vertices contained in the buffer
     * @param mutable     whether the buffer is expected to be updated after allocation (implementations may choose an
     *                    appropriate usage hint)
     */
    void allocate(ByteBuffer buffer, int vertexCount, boolean mutable);

    default void allocate(ByteBuffer buffer, boolean mutable) {
        allocate(buffer, getVertexFormat().getVertexCount(buffer), mutable);
    }
}
