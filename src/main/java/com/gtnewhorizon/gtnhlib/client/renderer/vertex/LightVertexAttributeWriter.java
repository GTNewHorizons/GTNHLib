package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

public final class LightVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | LIGHT_INDEX]);
        return 4;
    }
}
