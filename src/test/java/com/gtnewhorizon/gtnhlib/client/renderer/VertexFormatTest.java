package com.gtnewhorizon.gtnhlib.client.renderer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class VertexFormatTest {

    @Test
    void testDefaultVertexFormats() {
        assertEquals(VertexFlags.BITSET_SIZE, DefaultVertexFormat.ALL_FORMATS.length);
        for (VertexFormat format : DefaultVertexFormat.ALL_FORMATS) {
            assertNotNull(format);
        }
    }
}
