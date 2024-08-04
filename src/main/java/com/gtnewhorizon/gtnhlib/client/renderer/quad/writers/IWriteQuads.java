package com.gtnewhorizon.gtnhlib.client.renderer.quad.writers;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;

public interface IWriteQuads {

    void writeQuad(QuadView quad, ByteBuffer buf);
}
