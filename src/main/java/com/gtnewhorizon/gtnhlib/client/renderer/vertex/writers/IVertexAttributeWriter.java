package com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers;

import net.minecraft.client.renderer.Tessellator;

public interface IVertexAttributeWriter {

    int writeAttribute(long pointer, int[] data, int index);

    int writeAttribute(long pointer, Tessellator tessellator);

    // Populate the Tessellator using the buffer's contents
    int readAttribute(long pointer, Tessellator tessellator);
}
