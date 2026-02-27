package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix4fc;

public interface IVertexAttributeWriter {

    int writeAttribute(long pointer, int[] data, int index);

    int writeAttribute(long pointer, Tessellator tessellator);

    // Populate the Tessellator using the buffer's contents
    int readAttribute(long pointer, Tessellator tessellator);

    default int writeAttributeTransformed(long pointer, int[] data, int index, Matrix4fc transform) {
        return writeAttribute(pointer, data, index);
    }
}
