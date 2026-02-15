package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import net.minecraft.client.renderer.Tessellator;

public final class TextureVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | TEX_X_INDEX]);
        memPutInt(pointer + 4, data[index | TEX_Y_INDEX]);
        return 8;
    }

    @Override
    public int writeAttribute(long pointer, Tessellator tessellator) {
        memPutFloat(pointer, (float) tessellator.textureU);
        memPutFloat(pointer + 4, (float) tessellator.textureV);
        return 8;
    }

    @Override
    public int readAttribute(long pointer, Tessellator tessellator) {
        tessellator.textureU = memGetFloat(pointer);
        tessellator.textureV = memGetFloat(pointer + 4);
        return 8;
    }
}
