package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class PositionVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | X_INDEX]);
        memPutInt(pointer + 4, data[index | Y_INDEX]);
        memPutInt(pointer + 8, data[index | Z_INDEX]);
        return 12;
    }

    @Override
    public int writeAttributeTransformed(long pointer, int[] data, int index, Matrix4fc m, Vector3f v) {
        v.set(
                Float.intBitsToFloat(data[index | X_INDEX]),
                Float.intBitsToFloat(data[index | Y_INDEX]),
                Float.intBitsToFloat(data[index | Z_INDEX]));
        v.mulPosition(m);
        memPutFloat(pointer, v.x);
        memPutFloat(pointer + 4, v.y);
        memPutFloat(pointer + 8, v.z);
        return 12;
    }

    @Override
    public int writeAttribute(long pointer, Tessellator tessellator) {
        throw new UnsupportedOperationException("Cannot write position to tessellator!");
    }

    @Override
    public int readAttribute(long pointer, Tessellator tessellator) {
        throw new UnsupportedOperationException("Cannot read position from tessellator!");
    }

}
