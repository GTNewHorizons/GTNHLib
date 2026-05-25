package com.gtnewhorizon.gtnhlib.client.renderer.shader;

public final class SimpleShaderDefine implements IShaderDefinesInjector {
    private final String output;

    public SimpleShaderDefine(String name, Object value) {
        this.output = "#define " + name + ' ' + value;
    }

    @Override
    public void writeDefines(StringBuilder out) {
        writeLine(out, output);
    }
}
