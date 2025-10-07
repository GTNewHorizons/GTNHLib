package com.gtnewhorizon.gtnhlib.client.renderer.quad.writers;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import java.nio.ByteBuffer;

public interface IWriteQuads {

    void writeQuad(ModelQuadView quad, ByteBuffer buf);
}
