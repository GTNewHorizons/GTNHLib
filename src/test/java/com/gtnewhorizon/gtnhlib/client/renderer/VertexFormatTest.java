package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VertexFormatTest {

    @Test
    void testDefaultVertexFormats() {
        assertEquals(VertexFlags.BITSET_SIZE, DefaultVertexFormat.ALL_FORMATS.length);
        for (int i = 0; i < DefaultVertexFormat.ALL_FORMATS.length; i++) {
            final VertexFormat format = DefaultVertexFormat.ALL_FORMATS[i];
            assertNotNull(format);
            assertEquals(i, format.getVertexFlags());
        }
    }

    @Test
    void testWriteToBuffer0TransformDirect() {
        VertexFormat fmt = DefaultVertexFormat.POSITION;

        // Hand-craft a raw buffer: 2 vertices, 8 ints each
        int[] raw = new int[16];
        // Vertex 0 at (1, 2, 3)
        raw[0] = Float.floatToRawIntBits(1f);
        raw[1] = Float.floatToRawIntBits(2f);
        raw[2] = Float.floatToRawIntBits(3f);
        // Vertex 1 at (4, 5, 6)
        raw[8] = Float.floatToRawIntBits(4f);
        raw[9] = Float.floatToRawIntBits(5f);
        raw[10] = Float.floatToRawIntBits(6f);

        Matrix4f transform = new Matrix4f().translation(100, 200, 300);

        ByteBuffer buf = ByteBuffer.allocateDirect(256);
        long ptr = memAddress0(buf);
        long end = fmt.writeToBuffer0(ptr, raw, 16, transform, new Vector3f());

        int stride = fmt.getVertexSize();
        assertEquals(ptr + 2 * stride, end);

        // Vertex 0: (1+100, 2+200, 3+300)
        assertEquals(101f, memGetFloat(ptr), 0.0001f);
        assertEquals(202f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(303f, memGetFloat(ptr + 8), 0.0001f);

        // Vertex 1: (4+100, 5+200, 6+300)
        assertEquals(104f, memGetFloat(ptr + stride), 0.0001f);
        assertEquals(205f, memGetFloat(ptr + stride + 4), 0.0001f);
        assertEquals(306f, memGetFloat(ptr + stride + 8), 0.0001f);
    }
}
