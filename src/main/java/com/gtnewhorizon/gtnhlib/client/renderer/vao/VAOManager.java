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
     * Don't blindly cast it to a VertexArrayBuffer, your code shouldn't need to know if it's a VBO/VAO anyway
     */
    public static VertexBuffer createVAO(VertexFormat format, int drawMode) {
        return vaoEnabled ? new VertexArrayBuffer(format, drawMode) : new VertexBuffer(format, drawMode);
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
