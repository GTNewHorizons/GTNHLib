package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import net.minecraft.client.renderer.Tessellator;

public final class PositionVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | X_INDEX]);
        memPutInt(pointer + 4, data[index | Y_INDEX]);
        memPutInt(pointer + 8, data[index | Z_INDEX]);
        return 12;
    }

    @Override
    public int writeAttribute(long pointer, Tessellator tessellator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readAttribute(long pointer, Tessellator tessellator) {
        throw new UnsupportedOperationException();
    }

}
