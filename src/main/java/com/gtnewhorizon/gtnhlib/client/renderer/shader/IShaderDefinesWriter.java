package com.gtnewhorizon.gtnhlib.client.renderer.shader;

public interface IShaderDefinesWriter {

    void writeDefines(StringBuilder out);

    default void writeLine(StringBuilder out, String variable) {
        out.append(variable).append('\n');
    }
}
