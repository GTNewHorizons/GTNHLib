package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VAOManager {

    private static boolean vaoDisabled = false;
    public static final VaoFunctions VAO = UniversalVAO.getImplementation();

    /**
     * Creates a VAO if they are supported, and a VBO otherwise. <br>
     * If you need a VertexArrayBuffer in your code, make sure to only cast it if isVaoDisabled is false
     */
    public static VertexBuffer createVAO(VertexFormat format, int drawMode) {
        return vaoDisabled ? new VertexBuffer(format, drawMode) : new VertexArrayBuffer(format, drawMode);
    }

    public static boolean isVaoDisabled() {
        return vaoDisabled;
    }

    /**
     * Use this for any Driver/GPU that doesn't fully support VAO's.
     */
    public static void disableVAO() {
        vaoDisabled = true;
    }
}
