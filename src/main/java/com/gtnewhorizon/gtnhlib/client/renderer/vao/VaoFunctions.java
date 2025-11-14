package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import java.nio.IntBuffer;

public interface VaoFunctions {

    int getCurrentBinding();

    int glGenVertexArrays();

    void glGenVertexArrays(IntBuffer output);

    void glDeleteVertexArrays(int id);

    void glDeleteVertexArrays(IntBuffer ids);

    boolean glIsVertexArray(int id);

    void glBindVertexArray(int id);
}
