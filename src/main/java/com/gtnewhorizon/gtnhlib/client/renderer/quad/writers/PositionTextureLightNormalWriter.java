package com.gtnewhorizon.gtnhlib.client.renderer.quad.writers;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

public class PositionTextureLightNormalWriter implements IWriteQuads {

    private boolean direct;

    public PositionTextureLightNormalWriter() {
        init(false);
    }

    public void init(boolean direct) {
        this.direct = direct;
    }

    @Override
    public void writeQuad(ModelQuadView quad, ByteBuffer buf) {
        if (direct) {
            writeQuadDirect(quad, buf);
        } else {
            writeQuadIndirect(quad, buf);
        }
    }

    protected void writeQuadDirect(ModelQuadView quad, ByteBuffer buf) {
        throw new UnsupportedOperationException("Direct mode not supported yet");
    }

    protected void writeQuadIndirect(ModelQuadView quad, ByteBuffer buf) {
        for (int idx = 0; idx < 4; ++idx) {
            // Position (3 floats)
            buf.putFloat(quad.getX(idx));
            buf.putFloat(quad.getY(idx));
            buf.putFloat(quad.getZ(idx));

            // Texture (2 floats)
            buf.putFloat(quad.getTexU(idx));
            buf.putFloat(quad.getTexV(idx));

            // Light/Brightness (2 shorts packed as int)
            buf.putInt(quad.getLight(idx));

            // Normal + Padding (int)
            buf.putInt(quad.getForgeNormal(idx));
        }
    }
}
