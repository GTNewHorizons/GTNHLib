package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.*;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix4fc;
import org.joml.Vector3f;

public final class NormalVertexAttributeWriter implements IVertexAttributeWriter {

    @Override
    public int writeAttribute(long pointer, int[] data, int index) {
        memPutInt(pointer, data[index | NORMAL_INDEX]);
        return 4;
    }

    @Override
    public int writeAttributeTransformed(long pointer, int[] data, int index, Matrix4fc m, Vector3f n) {
        int packed = data[index | NORMAL_INDEX];
        n.set((byte) (packed) / 127f, (byte) (packed >> 8) / 127f, (byte) (packed >> 16) / 127f);
        n.mulDirection(m).normalize();

        memPutInt(
                pointer,
                ((byte) (n.x * 127)) & 0xFF | (((byte) (n.y * 127)) & 0xFF) << 8 | (((byte) (n.z * 127)) & 0xFF) << 16);
        return 4;
    }

    @Override
    public int writeAttribute(long pointer, Tessellator tessellator) {
        memPutInt(pointer, tessellator.normal);
        return 4;
    }

    @Override
    public int readAttribute(long pointer, Tessellator tessellator) {
        tessellator.normal = memGetInt(pointer);
        return 4;
    }
}
