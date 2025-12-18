package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

public final class TextureVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | TEX_X_INDEX]);
        memPutInt(pointer + 4, data[index | TEX_Y_INDEX]);
        return 8;
    }
}
