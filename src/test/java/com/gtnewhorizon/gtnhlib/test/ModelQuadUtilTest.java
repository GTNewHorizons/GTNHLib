package com.gtnewhorizon.gtnhlib.test;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ModelQuadUtilTest {

    @Test
    void testConstants() {
        assertTrue(isPowerOfTwo(ModelQuadUtil.VERTEX_SIZE));
        assertEquals(2 * ModelQuadUtil.VERTEX_SIZE, ModelQuadUtil.vertexOffset(2));
    }

    boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}
