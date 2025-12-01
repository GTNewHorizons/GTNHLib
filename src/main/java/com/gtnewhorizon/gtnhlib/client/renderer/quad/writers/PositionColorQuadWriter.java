package com.gtnewhorizon.gtnhlib.client.renderer.quad.writers;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

public class PositionColorQuadWriter implements IWriteQuads {

    private boolean direct;

    public PositionColorQuadWriter() {
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

            // Color (4 ubytes packed as int)
            buf.putInt(quad.getColor(idx));
        }
    }
}
