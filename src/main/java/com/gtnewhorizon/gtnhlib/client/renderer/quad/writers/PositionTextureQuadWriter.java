package com.gtnewhorizon.gtnhlib.client.renderer.quad.writers;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;

public class PositionTextureQuadWriter implements IWriteQuads {

    private boolean direct;

    public PositionTextureQuadWriter() {
        init(false);
    }

    public void init(boolean direct) {
        // This would need to be re-initialized if the direct vs indirect changes
        this.direct = direct;
    }

    @Override
    public void writeQuad(QuadView quad, ByteBuffer buf) {
        if (direct) {
            writeQuadDirect(quad, buf);
        } else {
            writeQuadIndirect(quad, buf);
        }
    }

    protected void writeQuadDirect(QuadView quad, ByteBuffer buf) {
        throw new UnsupportedOperationException("Direct mode not supported yet");
    }

    protected void writeQuadIndirect(QuadView quad, ByteBuffer buf) {
        for (int idx = 0; idx < 4; ++idx) {
            // Position
            buf.putFloat(quad.getX(idx));
            buf.putFloat(quad.getY(idx));
            buf.putFloat(quad.getZ(idx));

            // Texture
            buf.putFloat(quad.getTexU(idx));
            buf.putFloat(quad.getTexV(idx));
        }
    }
}
