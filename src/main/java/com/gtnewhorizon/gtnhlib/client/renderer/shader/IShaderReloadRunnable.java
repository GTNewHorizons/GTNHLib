package com.gtnewhorizon.gtnhlib.client.renderer.shader;

public interface IShaderReloadRunnable {

    void run(ShaderProgram shader);

    IShaderDefinesInjector[] getDefines();
}
