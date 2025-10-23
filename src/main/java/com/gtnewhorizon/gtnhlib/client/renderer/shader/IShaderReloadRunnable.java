package com.gtnewhorizon.gtnhlib.client.renderer.shader;

import java.nio.file.Path;

public interface IShaderReloadRunnable {

    void run(ShaderProgram shader, Path vertexPath, Path fragmentPath);
}
