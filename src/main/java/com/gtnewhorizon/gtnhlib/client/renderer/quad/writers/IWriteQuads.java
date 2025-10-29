package com.gtnewhorizon.gtnhlib.client.renderer.quad.writers;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

public interface IWriteQuads {

    void writeQuad(ModelQuadView quad, ByteBuffer buf);
}
