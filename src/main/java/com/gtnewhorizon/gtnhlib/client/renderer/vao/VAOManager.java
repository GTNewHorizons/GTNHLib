package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

/**
 * VAO/VBO creation helpers. <br>
 * There are two categories of buffers:
 *
 * <h3>Mutable buffers (glBufferData)</h3> - Backed by resizable storage. <br>
 * - Can be reallocated at any time. <br>
 * - Suitable for frequently changing geometry or unknown sizes. <br>
 * - Slightly more driver overhead.
 *
 * <h3>Immutable buffers (glBufferStorage)</h3> - Backed by fixed-size storage. <br>
 * - Allocation can happen only once. - Usually faster and more predictable for static or long-lived data. <br>
 * - Use GL_DYNAMIC_STORAGE_BIT if updates are needed. <br>
 */
public final class VAOManager {

    // true by default, false if disabled/unsupported
    private static boolean vaoEnabled;

    private static final boolean vaoUnsupported;
    public static final VaoFunctions VAO;

    // Old API
    @Deprecated
    public static VertexBuffer createVAO(VertexFormat format, int drawMode) {
        return vaoEnabled ? new VertexArrayBuffer(format, drawMode) : new VertexBuffer(format, drawMode);
    }

    private static IVertexBuffer createStorageVBO(VertexFormat format, int drawMode, int flags) {
        if (GLCaps.bufferStorageSupported()) {
            return new VertexBufferStorage(format, drawMode, flags);
        }

        return new VertexBuffer(format, drawMode);
    }

    private static IVertexBuffer allocateStorageVBO(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount,
            int flags) {
        if (GLCaps.bufferStorageSupported()) {
            return new VertexBufferStorage(format, drawMode, data, vertexCount, flags);
        }

        return new VertexBuffer(format, drawMode, data, vertexCount);
    }

    public static IVertexArrayObject vao(IVertexBuffer vbo) {
        return vaoEnabled ? new BaseVAO(vbo) : new VertexArrayUnsupported(vbo);
    }

    public static IVertexArrayObject vao(IVertexBuffer vbo, IndexBuffer ebo) {
        return new IndexedVAO(vbo, ebo); // TODO add VAO unsupported compat if it ever becomes an issue (likely won't)
    }

    // --------------- Mutable VAO's ---------------

    /**
     * Creates a mutable VBO, wrapped inside a VAO.
     */
    public static IVertexArrayObject createMutableVAO(VertexFormat format, int drawMode) {
        return vao(new VertexBuffer(format, drawMode));
    }

    /**
     * Creates a mutable VBO, wrapped inside a VAO with an EBO.
     */
    public static IVertexArrayObject createMutableVAO(VertexFormat format, int drawMode, IndexBuffer ebo) {
        return vao(new VertexBuffer(format, drawMode), ebo);
    }

    /**
     * Creates & allocates a mutable VBO, wrapped inside a VAO.
     */
    public static IVertexArrayObject allocateMutableVAO(VertexFormat format, int drawMode, ByteBuffer data,
            int vertexCount) {
        return vao(new VertexBuffer(format, drawMode, data, vertexCount));
    }

    /**
     * Creates & allocates a mutable VBO, wrapped inside a VAO with an EBO.
     */
    public static IVertexArrayObject allocateMutableVAO(VertexFormat format, int drawMode, ByteBuffer data,
            int vertexCount, IndexBuffer ebo) {
        return vao(new VertexBuffer(format, drawMode, data, vertexCount), ebo);
    }

    // --------------- Immutable VAO's ---------------

    /**
     * Creates an immutable VBO, wrapped inside a VAO.
     * <p>
     * After the first {@link IVertexBuffer#allocate} call, the buffer can no longer be allocated again.
     *
     * @param flags Buffer storage flags passed to glBufferStorage. <br>
     *              Use {@code 0} for fully immutable GPU-only data. <br>
     *              Use {@code GL44.GL_DYNAMIC_STORAGE_BIT} to allow updates via {@link IVertexBuffer#update}.
     */
    public static IVertexArrayObject createStorageVAO(VertexFormat format, int drawMode, int flags) {
        return vao(createStorageVBO(format, drawMode, flags));
    }

    /**
     * Creates an immutable VBO, wrapped inside a VAO with an EBO.
     * <p>
     * After the first {@link IVertexBuffer#allocate} call, the buffer can no longer be allocated again.
     *
     * @param flags Buffer storage flags passed to glBufferStorage. <br>
     *              Use {@code 0} for fully immutable GPU-only data. <br>
     *              Use {@code GL44.GL_DYNAMIC_STORAGE_BIT} to allow updates via {@link IVertexBuffer#update}.
     */
    public static IVertexArrayObject createStorageVAO(VertexFormat format, int drawMode, int flags, IndexBuffer ebo) {
        return vao(createStorageVBO(format, drawMode, flags), ebo);
    }

    /**
     * Creates & allocates an immutable VBO, wrapped inside a VAO.
     * <p>
     * This buffer can no longer be reallocated again with {@link IVertexBuffer#allocate}.
     *
     * @param flags Buffer storage flags passed to glBufferStorage. <br>
     *              Use {@code 0} for fully immutable GPU-only data. <br>
     *              Use {@code GL44.GL_DYNAMIC_STORAGE_BIT} to allow updates via {@link IVertexBuffer#update}.
     */
    public static IVertexArrayObject allocateStorageVAO(VertexFormat format, int drawMode, ByteBuffer data,
            int vertexCount, int flags) {
        return vao(allocateStorageVBO(format, drawMode, data, vertexCount, flags));
    }

    /**
     * Creates & allocates an immutable VBO, wrapped inside a VAO with an EBO.
     * <p>
     * This buffer can no longer be reallocated again with {@link IVertexBuffer#allocate}.
     *
     * @param flags Buffer storage flags passed to glBufferStorage. <br>
     *              Use {@code 0} for fully immutable GPU-only data. <br>
     *              Use {@code GL44.GL_DYNAMIC_STORAGE_BIT} to allow updates via {@link IVertexBuffer#update}.
     */
    public static IVertexArrayObject allocateStorageVAO(VertexFormat format, int drawMode, ByteBuffer data,
            int vertexCount, int flags, IndexBuffer ebo) {
        return vao(allocateStorageVBO(format, drawMode, data, vertexCount, flags), ebo);
    }

    public static boolean isVaoEnabled() {
        return vaoEnabled;
    }

    public static void disableVao() {
        vaoEnabled = false;
    }

    public static void enableVao() {
        vaoEnabled = !vaoUnsupported; // Only allow VAO's to be enabled if they are supported
    }

    static {
        VAO = GLCaps.VAO;
        vaoUnsupported = VAO == null;
        vaoEnabled = !vaoUnsupported;
    }
}
