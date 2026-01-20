package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.client.opengl.GLCaps;
import com.gtnewhorizon.gtnhlib.client.opengl.UniversalVAO;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IEmptyVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public final class VAOManager {

    // true by default, false if disabled/unsupported
    static boolean vaoEnabled;

    private static final boolean vaoUnsupported;
    public static final VaoFunctions VAO;

    @Deprecated // For clarity, use createMutableVAO / createStorageVAO instead
    public static VertexBuffer createVAO(VertexFormat format, int drawMode) {
        return createMutableVAO(format, drawMode);
    }

    /**
     * Creates a VAO if they are supported, and a VBO otherwise. <br>
     * Don't blindly cast it to a VertexArrayBuffer, your code shouldn't need to know if it's a VBO/VAO anyway
     */
    public static VertexBuffer createMutableVAO(VertexFormat format, int drawMode) {
        return vaoEnabled ? new VertexArrayBuffer(format, drawMode) : new VertexBuffer(format, drawMode);
    }

    public static int getStorageFlags(boolean mutable) {
        return mutable ? GL44.GL_DYNAMIC_STORAGE_BIT : 0;
    }

    public static IEmptyVertexBuffer createStorageVAO(VertexFormat format, int drawMode) {
        if (GLCaps.bufferStorageSupported()) {
            return vaoEnabled ? new VertexArrayBufferStorage(format, drawMode)
                    : new VertexBufferStorage(format, drawMode);
        }

        return createMutableVAO(format, drawMode);
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
        VAO = UniversalVAO.getImplementation(GLContext.getCapabilities());
        vaoUnsupported = VAO == null;
        vaoEnabled = !vaoUnsupported;
    }
}
