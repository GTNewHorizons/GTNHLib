package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.junit.jupiter.api.*;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public class DirectTessellatorTest {

    private static class TransformingTessellator extends DirectTessellator {

        Matrix4fc vertexTransform;
        private final Vector3f scratch = new Vector3f();

        TransformingTessellator(ByteBuffer initial) {
            super(initial);
        }

        @Override
        protected long writeVertexData(VertexFormat format, int[] rawBuffer, int rawBufferIndex) {
            if (vertexTransform != null) {
                return format.writeToBuffer0(writePtr, rawBuffer, rawBufferIndex, vertexTransform, scratch);
            }
            return super.writeVertexData(format, rawBuffer, rawBufferIndex);
        }
    }

    private DirectTessellator tess;
    private ByteBuffer initialBuffer;

    @BeforeEach
    void setup() {
        initialBuffer = ByteBuffer.allocateDirect(1024); // initial direct buffer
        tess = new DirectTessellator(initialBuffer);
        Tessellator.instance.isDrawing = false;
    }

    @Test
    void testAddVertexBasic() {
        tess.startDrawing(0);
        tess.addVertex(1, 2, 3);
        tess.addVertex(4, 5, 6);

        assertEquals(2, tess.vertexCount, "Vertex count should be 2");
        assertTrue(tess.bufferLimit() > 0, "Data size should be > 0");
    }

    @Test
    void testVertexFormatChangeMidBuffer() {
        tess.startDrawing(0);
        tess.addVertex(1, 1, 1); // old format (no color)

        assertEquals(DefaultVertexFormat.POSITION, tess.getVertexFormat());

        // Enable color → triggers fixBufferFormat
        tess.setColorRGBA(255, 128, 64, 32);
        tess.addVertex(2, 2, 2);

        assertEquals(DefaultVertexFormat.POSITION_COLOR, tess.getVertexFormat());

        // Verify vertex count
        assertEquals(2, tess.vertexCount);

        // Verify that writePtr advanced
        assertTrue(tess.bufferLimit() > 0, "Data size should increase after vertex with color");

        // Verify buffer contents for old vertex still has correct position
        long ptr = tess.startPtr;
        float x0 = memGetFloat(ptr);
        float y0 = memGetFloat(ptr + 4);
        float z0 = memGetFloat(ptr + 8);

        assertEquals(1.0f, x0, 0.0001);
        assertEquals(1.0f, y0, 0.0001);
        assertEquals(1.0f, z0, 0.0001);

        // Verify second vertex
        ptr = tess.startPtr + tess.getVertexFormat().getVertexSize(); // offset to second vertex
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
        assertEquals(tess.startPtr, tess.writePtr, "Write pointer should be reset");
    }

    @Test
    void testAllocateBufferCopy() {
        tess.startDrawing(0);
        tess.addVertex(1, 2, 3);
        tess.addVertex(4, 5, 6);

        ByteBuffer copy = tess.allocateBufferCopy();
        assertEquals(tess.bufferLimit(), copy.limit(), "Copy buffer size should match data size");

        float x0 = memGetFloat(memAddress0(copy));
        assertEquals(1.0f, x0, 0.0001);
        memFree(copy);
    }

    @Test
    void testEnsureCapacityPreservesData() {
        // Small initial buffer to force reallocation
        ByteBuffer small = ByteBuffer.allocateDirect(64);
        tess = new CallbackTessellator(small);

        tess.startDrawing(0);

        // Add enough vertices to force at least one realloc
        for (int i = 0; i < 10; i++) {
            tess.addVertex(i, i + 1, i + 2);
        }

        assertEquals(10, tess.vertexCount);

        long ptr = tess.startPtr;

        // Verify all vertices survived reallocation
        for (int i = 0; i < 10; i++) {
            float x = memGetFloat(ptr);
            float y = memGetFloat(ptr + 4);
            float z = memGetFloat(ptr + 8);

            assertEquals(i, x, 0.0001f);
            assertEquals(i + 1, y, 0.0001f);
            assertEquals(i + 2, z, 0.0001f);

            ptr += tess.getVertexFormat().getVertexSize();
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
        long ptr = tess.startPtr;
        for (int i = 0; i < 5; i++) {
            assertEquals(i, memGetFloat(ptr), 0.0001f);
            assertEquals(i, memGetFloat(ptr + 4), 0.0001f);
            assertEquals(i, memGetFloat(ptr + 8), 0.0001f);
            ptr += tess.getVertexFormat().getVertexSize();
        }
    }

    @Test
    void testDrawCallbackResetBehavior() {
        AtomicBoolean callbackCalled = new AtomicBoolean(false);

        final CallbackTessellator tess = new CallbackTessellator(initialBuffer);
        tess.setDrawCallback(t -> {
            callbackCalled.set(true);
            return true; // request reset
        });

        tess.startDrawing(0);
        tess.addVertex(1, 2, 3);

        int bytes = tess.draw();

        assertTrue(callbackCalled.get());
        assertEquals(0, tess.vertexCount);
        assertEquals(tess.startPtr, tess.writePtr);
        assertTrue(bytes > 0);
    }

    @Test
    void testInterceptDrawCopiesVertices() {
        Tessellator vanilla = Tessellator.instance;
        final CallbackTessellator tess = new CallbackTessellator(initialBuffer);
        tess.setDrawCallback((t) -> false);
        vanilla.startDrawing(0);
        vanilla.addVertex(1, 2, 3);
        vanilla.addVertex(4, 5, 6);

        int bytes = tess.interceptDraw(vanilla);

        assertEquals(2, tess.vertexCount);
        assertTrue(bytes > 0);

        long ptr = tess.startPtr;

        assertEquals(1f, memGetFloat(ptr), 0.0001f);
        assertEquals(2f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(3f, memGetFloat(ptr + 8), 0.0001f);

        ptr += tess.getVertexFormat().getVertexSize();

        assertEquals(4f, memGetFloat(ptr), 0.0001f);
        assertEquals(5f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(6f, memGetFloat(ptr + 8), 0.0001f);
    }

    @Test
    void testResetAfterReallocRestoresBaseBuffer() {
        ByteBuffer small = ByteBuffer.allocateDirect(32);
        tess = new DirectTessellator(small);

        tess.startDrawing(0);
        for (int i = 0; i < 5; i++) {
            tess.addVertex(i, i, i); // force realloc
        }

        assertTrue(tess.isResized());

        tess.reset();

        assertFalse(tess.isResized());
        assertEquals(tess.startPtr, tess.writePtr);
    }

    @Test
    void testAllocateBufferCopyIsIndependent() {
        tess.startDrawing(0);
        tess.addVertex(1, 2, 3);

        ByteBuffer copy = tess.allocateBufferCopy();

        // Modify original buffer
        memPutFloat(tess.startPtr, 99f);

        float copied = memGetFloat(memAddress0(copy));
        assertEquals(1f, copied, 0.0001f);

        memFree(copy);
    }

    @Test
    void testPreDefinedVertexFormat() {
        tess.setVertexFormat(DefaultVertexFormat.POSITION);
        tess.startDrawing(0);
        tess.setColorRGBA(255, 255, 255, 255);
        assertFalse(tess.hasColor);
    }

    @Test
    void testInterceptDrawWithTranslation() {
        Tessellator vanilla = Tessellator.instance;
        final TransformingTessellator ct = new TransformingTessellator(initialBuffer);

        ct.vertexTransform = new Matrix4f().translation(10, 20, 30);

        vanilla.startDrawing(0);
        vanilla.addVertex(1, 2, 3);
        vanilla.addVertex(4, 5, 6);

        ct.interceptDraw(vanilla);

        assertEquals(2, ct.vertexCount);

        long ptr = ct.startPtr;
        int stride = ct.getVertexFormat().getVertexSize();

        assertEquals(11f, memGetFloat(ptr), 0.0001f);
        assertEquals(22f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(33f, memGetFloat(ptr + 8), 0.0001f);

        ptr += stride;
        assertEquals(14f, memGetFloat(ptr), 0.0001f);
        assertEquals(25f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(36f, memGetFloat(ptr + 8), 0.0001f);
    }

    @Test
    void testInterceptDrawWithRotation() {
        Tessellator vanilla = Tessellator.instance;
        final TransformingTessellator ct = new TransformingTessellator(initialBuffer);

        // 90-degree rotation around Y axis: (x,y,z) -> (z, y, -x)
        ct.vertexTransform = new Matrix4f().rotationY((float) (Math.PI / 2));

        vanilla.startDrawing(0);
        vanilla.addVertex(1, 0, 0);

        ct.interceptDraw(vanilla);

        long ptr = ct.startPtr;
        assertEquals(0f, memGetFloat(ptr), 0.001f); // z component of (1,0,0) rotated
        assertEquals(0f, memGetFloat(ptr + 4), 0.001f);
        assertEquals(-1f, memGetFloat(ptr + 8), 0.001f);
    }

    @Test
    void testInterceptDrawWithTransformAndNormals() {
        Tessellator vanilla = Tessellator.instance;
        final TransformingTessellator ct = new TransformingTessellator(initialBuffer);

        // 90-degree rotation around Z axis: normal (1,0,0) -> (0,1,0)
        ct.vertexTransform = new Matrix4f().rotationZ((float) (Math.PI / 2));

        vanilla.startDrawing(0);
        vanilla.setNormal(1, 0, 0);
        vanilla.addVertex(0, 0, 0);

        ct.interceptDraw(vanilla);

        assertTrue(ct.getVertexFormat().hasNormals());

        // Find normal bytes after position (12 bytes)
        // Format is POSITION_NORMAL: pos(12) + normal(4)
        long ptr = ct.startPtr + 12;
        byte nx = memGetByte(ptr);
        byte ny = memGetByte(ptr + 1);
        byte nz = memGetByte(ptr + 2);

        // After 90-deg Z rotation: (1,0,0) -> (0,1,0)
        assertEquals(0, nx, 2); // allow +-2 for rounding
        assertEquals(127, ny, 2);
        assertEquals(0, nz, 2);
    }

    @Test
    void testInterceptDrawNullTransformUnchanged() {
        Tessellator vanilla = Tessellator.instance;
        final TransformingTessellator ct = new TransformingTessellator(initialBuffer);

        // vertexTransform is null by default
        assertNull(ct.vertexTransform);

        vanilla.startDrawing(0);
        vanilla.addVertex(1, 2, 3);
        vanilla.addVertex(4, 5, 6);

        ct.interceptDraw(vanilla);

        assertEquals(2, ct.vertexCount);

        long ptr = ct.startPtr;

        assertEquals(1f, memGetFloat(ptr), 0.0001f);
        assertEquals(2f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(3f, memGetFloat(ptr + 8), 0.0001f);

        ptr += ct.getVertexFormat().getVertexSize();

        assertEquals(4f, memGetFloat(ptr), 0.0001f);
        assertEquals(5f, memGetFloat(ptr + 4), 0.0001f);
        assertEquals(6f, memGetFloat(ptr + 8), 0.0001f);
    }
}
