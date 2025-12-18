package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

public final class PositionVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | X_INDEX]);
        memPutInt(pointer + 4, data[index | Y_INDEX]);
        memPutInt(pointer + 8, data[index | Z_INDEX]);
        return 12;
    }
}
