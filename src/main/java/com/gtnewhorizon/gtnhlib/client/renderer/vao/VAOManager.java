package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL44;

import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public final class VAOManager {

    // true by default, false if disabled/unsupported
    private static boolean vaoEnabled;

    private static final boolean vaoUnsupported;
    public static final VaoFunctions VAO;

    private static IVertexArrayObject vao(IVertexBuffer vbo) {
        return vaoEnabled ? new BaseVAO(vbo) : new VertexArrayUnsupported(vbo);
    }

    private static IVertexArrayObject vao(IVertexBuffer vbo, IndexBuffer ebo) {
        return new IndexedVAO(vbo, ebo); // TODO
    }

    @Deprecated // For clarity, use createMutableVAO / createStorageVAO instead
    public static VertexBuffer createVAO(VertexFormat format, int drawMode) {
        return createMutableVAO(format, drawMode);
    }

    @Deprecated
    public static VertexBuffer createMutableVAO(VertexFormat format, int drawMode) {
        return vaoEnabled ? new VertexArrayBuffer(format, drawMode) : new VertexBuffer(format, drawMode);
    }

    public static IVertexArrayObject allocateMutableVAO(VertexFormat format, int drawMode, ByteBuffer data,
            int vertexCount) {
        return vao(new VertexBuffer(format, drawMode, data, vertexCount));
    }

    public static IVertexArrayObject allocateMutableVAO(VertexFormat format, int drawMode, ByteBuffer data,
            int vertexCount, IndexBuffer ebo) {
        return vao(new VertexBuffer(format, drawMode, data, vertexCount), ebo);
    }

    private static IVertexBuffer allocateStorageVBO(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount,
            int flags) {
        if (GLCaps.bufferStorageSupported()) {
            return new VertexBufferStorage(format, drawMode, data, vertexCount, flags);
        }

        return new VertexBuffer(format, drawMode, data, vertexCount);
    }

    public static IVertexArrayObject allocateStorageVAO(VertexFormat format, int drawMode, ByteBuffer data,
            int vertexCount, int flags) {
        return vao(allocateStorageVBO(format, drawMode, data, vertexCount, flags));
    }

    public static IVertexArrayObject allocateStorageVAO(VertexFormat format, int drawMode, ByteBuffer data,
            int vertexCount, int flags, IndexBuffer ebo) {
        return vao(allocateStorageVBO(format, drawMode, data, vertexCount, flags), ebo);
    }

    public static int getStorageFlags(boolean mutable) {
        return mutable ? GL44.GL_DYNAMIC_STORAGE_BIT : 0;
    }

    public static IVertexArrayObject createStorageVAO(VertexFormat format, int drawMode) {
        if (GLCaps.bufferStorageSupported()) {
            return vao(new VertexBufferStorage(format, drawMode));
        }

        return vao(new VertexBuffer(format, drawMode));
    }

    public static boolean isVaoEnabled() {
        return vaoEnabled;
    }

    public static void disableVao() {
        vaoEnabled = false;
    }

    // Only allow VAO's to be enabled if they are supported
    public static void enableVao() {
        vaoEnabled = !vaoUnsupported;
    }

    static {
        VAO = GLCaps.VAO;
        vaoUnsupported = VAO == null;
        vaoEnabled = !vaoUnsupported;
    }
}
