package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.renderer.Tessellator;

import org.junit.jupiter.api.*;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;

public class DirectTessellatorTest {

    private DirectTessellator tess;
    private ByteBuffer initialBuffer;

    @BeforeEach
    void setup() {
        initialBuffer = ByteBuffer.allocateDirect(1024); // initial direct buffer
        tess = new DirectTessellator(
                t -> true, // Always reset
                initialBuffer);
    }

    @Test
    void testAddVertexBasic() {
        tess.startDrawing(0);
        tess.addVertex(1, 2, 3);
        tess.addVertex(4, 5, 6);

        assertEquals(2, tess.vertexCount, "Vertex count should be 2");
        assertTrue(tess.getDataSize() > 0, "Data size should be > 0");
    }

    @Test
    void testVertexFormatChangeMidBuffer() {
        tess.startDrawing(0);
        tess.addVertex(1, 1, 1); // old format (no color)

        assertEquals(DefaultVertexFormat.POSITION, tess.format);

        // Enable color → triggers fixBufferFormat
        tess.setColorRGBA(255, 128, 64, 32);
        tess.addVertex(2, 2, 2);

        assertEquals(DefaultVertexFormat.POSITION_COLOR, tess.format);

        // Verify vertex count
        assertEquals(2, tess.vertexCount);

        // Verify that writePtr advanced
        assertTrue(tess.getDataSize() > 0, "Data size should increase after vertex with color");

        // Verify buffer contents for old vertex still has correct position
        long ptr = tess.baseBufferAddress;
        float x0 = memGetFloat(ptr);
        float y0 = memGetFloat(ptr + 4);
        float z0 = memGetFloat(ptr + 8);

        assertEquals(1.0f, x0, 0.0001);
        assertEquals(1.0f, y0, 0.0001);
        assertEquals(1.0f, z0, 0.0001);

        // Verify second vertex
        ptr = tess.baseBufferAddress + tess.format.getVertexSize(); // offset to second vertex
        float x1 = memGetFloat(ptr);
        float y1 = memGetFloat(ptr + 4);
        float z1 = memGetFloat(ptr + 8);

        assertEquals(2.0f, x1, 0.0001);
        assertEquals(2.0f, y1, 0.0001);
        assertEquals(2.0f, z1, 0.0001);
    }

    @Test
    void testReset() {
        tess.startDrawing(0);
        tess.addVertex(0, 0, 0);
        tess.reset();

        assertEquals(0, tess.vertexCount, "Vertex count should be reset");
        assertEquals(tess.baseBufferAddress, tess.writePtr, "Write pointer should be reset");
    }

    @Test
    void testAllocateBufferCopy() {
        tess.startDrawing(0);
        tess.addVertex(1, 2, 3);
        tess.addVertex(4, 5, 6);

        ByteBuffer copy = tess.allocateBufferCopy();
        assertEquals(tess.getDataSize(), copy.limit(), "Copy buffer size should match data size");

        float x0 = memGetFloat(memAddress0(copy));
        assertEquals(1.0f, x0, 0.0001);
        memFree(copy); // remember to free native buffer
    }

    @Test
    void testEnsureCapacityPreservesData() {
        // Small initial buffer to force reallocation
        ByteBuffer small = ByteBuffer.allocateDirect(64);
        tess = new DirectTessellator(t -> false, small);

        tess.startDrawing(0);

        // Add enough vertices to force at least one realloc
        for (int i = 0; i < 10; i++) {
            tess.addVertex(i, i + 1, i + 2);
        }

        assertEquals(10, tess.vertexCount);

        long ptr = tess.basePtr;

        // Verify all vertices survived reallocation
        for (int i = 0; i < 10; i++) {
            float x = memGetFloat(ptr);
            float y = memGetFloat(ptr + 4);
            float z = memGetFloat(ptr + 8);

            assertEquals(i, x, 0.0001f);
            assertEquals(i + 1, y, 0.0001f);
            assertEquals(i + 2, z, 0.0001f);

            ptr += tess.format.getVertexSize();
        }
    }

    @Test
    void testMultipleFormatChangesMidBuffer() {
        tess.startDrawing(0);

        tess.addVertex(0, 0, 0);

        tess.setColorRGBA(255, 0, 0, 255);
        tess.addVertex(1, 1, 1);

        tess.setTextureUV(0.5, 0.5);
        tess.addVertex(2, 2, 2);

        tess.setNormal(0, 1, 0);
        tess.addVertex(3, 3, 3);

        tess.setBrightness(0x00F000F0);
        tess.addVertex(4, 4, 4);

        assertEquals(5, tess.vertexCount);

        // Validate positions of all vertices
        long ptr = tess.baseBufferAddress;
        for (int i = 0; i < 5; i++) {
            assertEquals(i, memGetFloat(ptr), 0.0001f);
            assertEquals(i, memGetFloat(ptr + 4), 0.0001f);
            assertEquals(i, memGetFloat(ptr + 8), 0.0001f);
            ptr += tess.format.getVertexSize();
        }
    }

    @Test
    void testDrawCallbackResetBehavior() {
        AtomicBoolean callbackCalled = new AtomicBoolean(false);

        tess = new DirectTessellator(t -> {
            callbackCalled.set(true);
            return true; // request reset
        }, initialBuffer);

        tess.startDrawing(0);
        tess.addVertex(1, 2, 3);

        int bytes = tess.draw();

        assertTrue(callbackCalled.get());
        assertEquals(0, tess.vertexCount);
        assertEquals(tess.baseBufferAddress, tess.writePtr);
        assertTrue(bytes > 0);
    }

    @Test
    void testInterceptDrawCopiesVertices() {
        Tessellator vanilla = Tessellator.instance;
        tess.setDrawCallback((t) -> false);
        vanilla.startDrawing(0);
        vanilla.addVertex(1, 2, 3);
        vanilla.addVertex(4, 5, 6);

        int bytes = tess.interceptDraw(vanilla);

        assertEquals(2, tess.vertexCount);
        assertTrue(bytes > 0);

        long ptr = tess.baseBufferAddress;

        assertEquals(1f, memGetFloat(ptr), 0.0001f);
        assertEquals(2f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(3f, memGetFloat(ptr + 8), 0.0001f);

        ptr += tess.format.getVertexSize();

        assertEquals(4f, memGetFloat(ptr), 0.0001f);
        assertEquals(5f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(6f, memGetFloat(ptr + 8), 0.0001f);
    }

    @Test
    void testResetAfterReallocRestoresBaseBuffer() {
        ByteBuffer small = ByteBuffer.allocateDirect(32);
        tess = new DirectTessellator(t -> false, small);

        tess.startDrawing(0);
        for (int i = 0; i < 5; i++) {
            tess.addVertex(i, i, i); // force realloc
        }

        assertNotSame(small, tess.buffer);

        tess.reset();

        assertSame(small, tess.buffer);
        assertEquals(tess.baseBufferAddress, tess.writePtr);
    }

    @Test
    void testAllocateBufferCopyIsIndependent() {
        tess.startDrawing(0);
        tess.addVertex(1, 2, 3);

        ByteBuffer copy = tess.allocateBufferCopy();

        // Modify original buffer
        memPutFloat(tess.baseBufferAddress, 99f);

        float copied = memGetFloat(memAddress0(copy));
        assertEquals(1f, copied, 0.0001f);

        memFree(copy);
    }
}
