package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VAOManager {

    // true by default, false if disabled/unsupported
    private static boolean vaoEnabled;

    private static final boolean vaoUnsupported;
    public static final VaoFunctions VAO;

    /**
     * Creates a VAO if they are supported, and a VBO otherwise. <br>
     * If you need a VertexArrayBuffer in your code, make sure to only cast it if isVaoDisabled is false
     */
    public static VertexBuffer createVAO(VertexFormat format, int drawMode) {
        return vaoEnabled ? new VertexArrayBuffer(format, drawMode) : new VertexBuffer(format, drawMode);
    }

    public static boolean isVaoEnabled() {
        return vaoEnabled;
    }

    public static void disableVAO() {
        vaoEnabled = false;
    }

    // Only allow VAO's to be enabled if they are supported
    public static void enableVAO() {
        vaoEnabled = !vaoUnsupported;
    }

    static {
        VAO = UniversalVAO.getImplementation(GLContext.getCapabilities());
        vaoUnsupported = VAO == null;
        vaoEnabled = !vaoUnsupported;
    }
}
