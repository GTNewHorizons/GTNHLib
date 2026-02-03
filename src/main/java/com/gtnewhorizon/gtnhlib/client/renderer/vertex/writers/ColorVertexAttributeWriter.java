package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import net.minecraft.client.renderer.Tessellator;

public final class ColorVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | COLOR_INDEX]);
        return 4;
    }

    @Override
    public int writeAttribute(long pointer, Tessellator tessellator) {
        memPutInt(pointer, tessellator.color);
        return 4;
    }

    @Override
    public int readAttribute(long pointer, Tessellator tessellator) {
        tessellator.color = memGetInt(pointer);
        return 4;
    }
}
