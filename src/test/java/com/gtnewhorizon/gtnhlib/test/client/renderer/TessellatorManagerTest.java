package com.gtnewhorizon.gtnhlib.test.client.renderer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive.ModelPrimitiveView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;

/**
 * Unit tests for TessellatorManager compiling mode and capturing mode.
 */
public class TessellatorManagerTest {

    @AfterEach
    void cleanup() {
        // Ensure clean state after each test
        TessellatorManager.cleanup();
    }

    @Test
    void testSetCompilingWithNullCallback() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TessellatorManager.setCompiling(null),
                "setCompiling(null) should throw IllegalArgumentException");
    }

    @Test
    void testNestedCompiling() {
        // Nested COMPILING is now ALLOWED for nested display list compilation
        TessellatorManager.setCompiling((quads, primitives, flags) -> {});
        assertDoesNotThrow(
                () -> TessellatorManager.setCompiling((quads, primitives, flags) -> {}),
                "Nested setCompiling() should be allowed");
        TessellatorManager.stopCompiling(); // Stop inner
        TessellatorManager.stopCompiling(); // Stop outer
    }

    @Test
    void testNestedCompilingCallbacks() {
        // Test that nested COMPILING levels have independent callbacks
        final java.util.List<String> callOrder = new java.util.ArrayList<>();

        // Outer compilation
        TessellatorManager.setCompiling((quads, primitives, flags) -> { callOrder.add("outer"); });

        CapturingTessellator outerTess = (CapturingTessellator) TessellatorManager.get();
        outerTess.startDrawingQuads();
        outerTess.pos(0, 0, 0).tex(0, 0).endVertex();
        outerTess.pos(1, 0, 0).tex(1, 0).endVertex();
        outerTess.pos(1, 1, 0).tex(1, 1).endVertex();
        outerTess.pos(0, 1, 0).tex(0, 1).endVertex();
        outerTess.draw(); // Should trigger outer callback

        assertEquals(1, callOrder.size(), "Outer callback should have been called once");
        assertEquals("outer", callOrder.get(0));

        // Nested inner compilation
        TessellatorManager.setCompiling((quads, primitives, flags) -> { callOrder.add("inner"); });

        CapturingTessellator innerTess = (CapturingTessellator) TessellatorManager.get();
        assertSame(outerTess, innerTess, "Should reuse same tessellator instance");

        innerTess.startDrawingQuads();
        innerTess.pos(2, 0, 0).tex(0, 0).endVertex();
        innerTess.pos(3, 0, 0).tex(1, 0).endVertex();
        innerTess.pos(3, 1, 0).tex(1, 1).endVertex();
        innerTess.pos(2, 1, 0).tex(0, 1).endVertex();
        innerTess.draw(); // Should trigger inner callback (not outer)

        assertEquals(2, callOrder.size(), "Inner callback should have been called");
        assertEquals("inner", callOrder.get(1), "Most recent call should be inner");

        TessellatorManager.stopCompiling(); // Stop inner

        // Back in outer context - draws should trigger outer callback again
        outerTess.startDrawingQuads();
        outerTess.pos(4, 0, 0).tex(0, 0).endVertex();
        outerTess.pos(5, 0, 0).tex(1, 0).endVertex();
        outerTess.pos(5, 1, 0).tex(1, 1).endVertex();
        outerTess.pos(4, 1, 0).tex(0, 1).endVertex();
        outerTess.draw(); // Should trigger outer callback

        assertEquals(3, callOrder.size());
        assertEquals("outer", callOrder.get(2), "After inner exits, outer callback should work again");

        TessellatorManager.stopCompiling(); // Stop outer

        // Verify call order
        assertEquals(
                java.util.Arrays.asList("outer", "inner", "outer"),
                callOrder,
                "Callbacks should be invoked in correct nested order");
    }

    @Test
    void testStopCompilingWhenNotCompiling() {
        assertThrows(
                IllegalStateException.class,
                TessellatorManager::stopCompiling,
                "stopCompiling() when not compiling should throw IllegalStateException");
    }

    @Test
    void testNestedCapturing() {
        TessellatorManager.startCapturing();
        assertDoesNotThrow(
                TessellatorManager::startCapturing,
                "startCapturing() when already capturing should be allowed for nesting");
        TessellatorManager.stopCapturingToPooledQuads();
        TessellatorManager.stopCapturingToPooledQuads();
    }

    @Test
    void testNestingCapturingInsideCompiling() {
        TessellatorManager.setCompiling((quads, primitives, flags) -> {});
        assertDoesNotThrow(
                TessellatorManager::startCapturing,
                "startCapturing() when compiling should be allowed for nesting");
        TessellatorManager.stopCapturingToPooledQuads();
        TessellatorManager.stopCompiling();
    }

    @Test
    void testSetCompilingAndStop() {
        // Basic lifecycle test - should not throw
        TessellatorManager.setCompiling((quads, primitives, flags) -> {});
        TessellatorManager.stopCompiling();
    }

    @Test
    void testCleanupClearsCompilingState() {
        TessellatorManager.setCompiling((quads, primitives, flags) -> {});
        TessellatorManager.cleanup();

        // After cleanup, compiling should be cleared
        // This means we should be able to set compiling again without error
        assertDoesNotThrow(() -> TessellatorManager.setCompiling((quads, primitives, flags) -> {}));
        TessellatorManager.stopCompiling();
    }

    @Test
    void testCapturingLifecycle() {
        TessellatorManager.startCapturing();
        List<ModelQuadViewMutable> quads = TessellatorManager.stopCapturingToPooledQuads();
        assertNotNull(quads, "stopCapturingToPooledQuads should return non-null list");
    }

    @Test
    void testStopCapturingWhenNotCapturing() {
        assertThrows(
                IllegalStateException.class,
                TessellatorManager::stopCapturingToPooledQuads,
                "stopCapturingToPooledQuads() when not capturing should throw IllegalStateException");
    }

    @Test
    void testNestedCapturingQuadIsolation() {
        // Start parent capture
        CapturingTessellator parentTess = TessellatorManager.startCapturingAndGet();
        parentTess.startDrawingQuads();
        parentTess.pos(0, 0, 0).tex(0, 0).endVertex();
        parentTess.pos(1, 0, 0).tex(1, 0).endVertex();
        parentTess.pos(1, 1, 0).tex(1, 1).endVertex();
        parentTess.pos(0, 1, 0).tex(0, 1).endVertex();
        parentTess.draw();

        // In index-based mode, quads accumulate in collectedQuads until nesting or stop
        assertEquals(1, parentTess.getQuads().size(), "After draw(), quads accumulate in collectedQuads");

        // Start nested child capture - parent's quads are preserved, list cleared for child
        CapturingTessellator childTess = TessellatorManager.startCapturingAndGet();
        assertSame(parentTess, childTess, "Should reuse same tessellator instance");
        assertEquals(0, childTess.getQuads().size(), "Child should start with 0 quads (parent's were saved)");

        childTess.startDrawingQuads();
        childTess.pos(2, 0, 0).tex(0, 0).endVertex();
        childTess.pos(3, 0, 0).tex(1, 0).endVertex();
        childTess.pos(3, 1, 0).tex(1, 1).endVertex();
        childTess.pos(2, 1, 0).tex(0, 1).endVertex();
        childTess.draw();

        List<ModelQuadViewMutable> childQuads = TessellatorManager.stopCapturingToPooledQuads();
        assertEquals(1, childQuads.size(), "Child should have captured 1 quad");
        assertEquals(2.0f, childQuads.get(0).getX(0), 0.001f, "Child quad should have correct position");

        // After popping child, parent continues with cleared list (its quads already saved)
        assertEquals(0, parentTess.getQuads().size(), "tess.getQuads() cleared for parent to continue");

        // Parent can continue capturing
        parentTess.startDrawingQuads();
        parentTess.pos(4, 0, 0).tex(0, 0).endVertex();
        parentTess.pos(5, 0, 0).tex(1, 0).endVertex();
        parentTess.pos(5, 1, 0).tex(1, 1).endVertex();
        parentTess.pos(4, 1, 0).tex(0, 1).endVertex();
        parentTess.draw();

        List<ModelQuadViewMutable> parentQuads = TessellatorManager.stopCapturingToPooledQuads();
        assertEquals(2, parentQuads.size(), "Parent should have 2 quads total");
        assertEquals(0.0f, parentQuads.get(0).getX(0), 0.001f, "First parent quad should have correct position");
        assertEquals(4.0f, parentQuads.get(1).getX(0), 0.001f, "Second parent quad should have correct position");
    }

    @Test
    void testMultiLevelNestedCapturing() {
        // Test 3-level nesting: grandparent → parent → child
        CapturingTessellator grandparent = TessellatorManager.startCapturingAndGet();
        grandparent.startDrawingQuads();
        grandparent.pos(0, 0, 0).tex(0, 0).endVertex();
        grandparent.pos(1, 0, 0).tex(1, 0).endVertex();
        grandparent.pos(1, 1, 0).tex(1, 1).endVertex();
        grandparent.pos(0, 1, 0).tex(0, 1).endVertex();
        grandparent.draw();

        CapturingTessellator parent = TessellatorManager.startCapturingAndGet();
        parent.startDrawingQuads();
        parent.pos(2, 0, 0).tex(0, 0).endVertex();
        parent.pos(3, 0, 0).tex(1, 0).endVertex();
        parent.pos(3, 1, 0).tex(1, 1).endVertex();
        parent.pos(2, 1, 0).tex(0, 1).endVertex();
        parent.draw();

        CapturingTessellator child = TessellatorManager.startCapturingAndGet();
        child.startDrawingQuads();
        child.pos(4, 0, 0).tex(0, 0).endVertex();
        child.pos(5, 0, 0).tex(1, 0).endVertex();
        child.pos(5, 1, 0).tex(1, 1).endVertex();
        child.pos(4, 1, 0).tex(0, 1).endVertex();
        child.draw();

        List<ModelQuadViewMutable> childQuads = TessellatorManager.stopCapturingToPooledQuads();
        assertEquals(1, childQuads.size(), "Child should have 1 quad");
        assertEquals(4.0f, childQuads.get(0).getX(0), 0.001f);

        List<ModelQuadViewMutable> parentQuads = TessellatorManager.stopCapturingToPooledQuads();
        assertEquals(1, parentQuads.size(), "Parent should have 1 quad");
        assertEquals(2.0f, parentQuads.get(0).getX(0), 0.001f);

        List<ModelQuadViewMutable> grandparentQuads = TessellatorManager.stopCapturingToPooledQuads();
        assertEquals(1, grandparentQuads.size(), "Grandparent should have 1 quad");
        assertEquals(0.0f, grandparentQuads.get(0).getX(0), 0.001f);
    }

    @Test
    void testFlagsEqualsAndHashCode() {
        // Test that Flags instances with identical values are equal
        CapturingTessellator.Flags flags1 = new CapturingTessellator.Flags(true, true, false, true);
        CapturingTessellator.Flags flags2 = new CapturingTessellator.Flags(true, true, false, true);
        CapturingTessellator.Flags flags3 = new CapturingTessellator.Flags(true, false, false, true);

        // Same values should be equal
        assertEquals(flags1, flags2, "Flags with identical values should be equal");
        assertEquals(flags1.hashCode(), flags2.hashCode(), "Equal flags should have same hashCode");

        // Different values should not be equal
        assertNotEquals(flags1, flags3, "Flags with different values should not be equal");

        // Reflexive
        assertEquals(flags1, flags1, "Flags should equal itself");

        // Copy constructor should produce equal flags
        CapturingTessellator.Flags flags4 = new CapturingTessellator.Flags(flags1);
        assertEquals(flags1, flags4, "Copy constructor should produce equal flags");
        assertEquals(flags1.hashCode(), flags4.hashCode(), "Copied flags should have same hashCode");
    }

    @Test
    void testNestedCapturingWithCompilingParent() {
        // Test COMPILING → CAPTURING → CAPTURING (Angelica use case)
        TessellatorManager.setCompiling((quads, primitives, flags) -> {
            // Compiling callback - start capturing inside
            CapturingTessellator parentTess = TessellatorManager.startCapturingAndGet();
            parentTess.startDrawingQuads();
            parentTess.pos(0, 0, 0).tex(0, 0).endVertex();
            parentTess.pos(1, 0, 0).tex(1, 0).endVertex();
            parentTess.pos(1, 1, 0).tex(1, 1).endVertex();
            parentTess.pos(0, 1, 0).tex(0, 1).endVertex();
            parentTess.draw();

            // Nest another capturing inside
            CapturingTessellator childTess = TessellatorManager.startCapturingAndGet();
            childTess.startDrawingQuads();
            childTess.pos(2, 0, 0).tex(0, 0).endVertex();
            childTess.pos(3, 0, 0).tex(1, 0).endVertex();
            childTess.pos(3, 1, 0).tex(1, 1).endVertex();
            childTess.pos(2, 1, 0).tex(0, 1).endVertex();
            childTess.draw();

            List<ModelQuadViewMutable> childQuads = TessellatorManager.stopCapturingToPooledQuads();
            assertEquals(1, childQuads.size());

            List<ModelQuadViewMutable> parentQuads = TessellatorManager.stopCapturingToPooledQuads();
            assertEquals(1, parentQuads.size());
        });

        TessellatorManager.stopCompiling();
    }

    @Test
    void testCompilingModeQuadsGoToQuadsList() {
        // In COMPILING mode, GL_QUADS should go to quads list, primitives should be empty
        final List<ModelQuadViewMutable> receivedQuads = new ArrayList<>();
        final List<ModelPrimitiveView> receivedPrimitives = new ArrayList<>();

        TessellatorManager.setCompiling((quads, primitives, flags) -> {
            receivedQuads.addAll(quads);
            receivedPrimitives.addAll(primitives);
        });

        CapturingTessellator tess = (CapturingTessellator) TessellatorManager.get();
        tess.startDrawingQuads();
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(1, 1, 0).tex(1, 1).endVertex();
        tess.pos(0, 1, 0).tex(0, 1).endVertex();
        tess.draw();

        TessellatorManager.stopCompiling();

        assertEquals(1, receivedQuads.size(), "COMPILING mode: GL_QUADS should produce 1 quad");
        assertTrue(receivedPrimitives.isEmpty(), "COMPILING mode: GL_QUADS should not produce primitives");
    }

    @Test
    void testCompilingModeTrianglesGoToPrimitivesList() {
        // In COMPILING mode, GL_TRIANGLES should go to primitives list (NOT quadrangulated)
        final List<ModelQuadViewMutable> receivedQuads = new ArrayList<>();
        final List<ModelPrimitiveView> receivedPrimitives = new ArrayList<>();

        TessellatorManager.setCompiling((quads, primitives, flags) -> {
            receivedQuads.addAll(quads);
            receivedPrimitives.addAll(primitives);
        });

        CapturingTessellator tess = (CapturingTessellator) TessellatorManager.get();
        tess.startDrawing(GL11.GL_TRIANGLES);
        // Triangle 1
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(0.5, 1, 0).tex(0.5, 1).endVertex();
        tess.draw();

        TessellatorManager.stopCompiling();

        assertTrue(receivedQuads.isEmpty(), "COMPILING mode: GL_TRIANGLES should not produce quads");
        assertEquals(1, receivedPrimitives.size(), "COMPILING mode: GL_TRIANGLES should produce 1 triangle primitive");
        assertEquals(3, receivedPrimitives.get(0).getVertexCount(), "Triangle should have 3 vertices");
    }

    @Test
    void testCompilingModeLinesGoToPrimitivesList() {
        // In COMPILING mode, GL_LINES should go to primitives list
        final List<ModelQuadViewMutable> receivedQuads = new ArrayList<>();
        final List<ModelPrimitiveView> receivedPrimitives = new ArrayList<>();

        TessellatorManager.setCompiling((quads, primitives, flags) -> {
            receivedQuads.addAll(quads);
            receivedPrimitives.addAll(primitives);
        });

        CapturingTessellator tess = (CapturingTessellator) TessellatorManager.get();
        tess.startDrawing(GL11.GL_LINES);
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 1, 1).tex(1, 1).endVertex();
        tess.draw();

        TessellatorManager.stopCompiling();

        assertTrue(receivedQuads.isEmpty(), "COMPILING mode: GL_LINES should not produce quads");
        assertEquals(1, receivedPrimitives.size(), "COMPILING mode: GL_LINES should produce 1 line primitive");
        assertEquals(2, receivedPrimitives.get(0).getVertexCount(), "Line should have 2 vertices");
    }

    @Test
    void testCapturingModeTrianglesAreQuadrangulated() {
        // In CAPTURING mode, GL_TRIANGLES should be quadrangulated into quads list for backward compat
        CapturingTessellator tess = TessellatorManager.startCapturingAndGet();
        tess.startDrawing(GL11.GL_TRIANGLES);
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(0.5, 1, 0).tex(0.5, 1).endVertex();
        tess.draw();

        List<ModelQuadViewMutable> quads = TessellatorManager.stopCapturingToPooledQuads();

        assertEquals(1, quads.size(), "CAPTURING mode: GL_TRIANGLES should be quadrangulated into 1 quad");
        // Quadrangulated triangle has v2 duplicated to v3
        assertEquals(quads.get(0).getX(2), quads.get(0).getX(3), 0.001f, "Quadrangulated: v2.x should equal v3.x");
        assertEquals(quads.get(0).getY(2), quads.get(0).getY(3), 0.001f, "Quadrangulated: v2.y should equal v3.y");
    }

    @Test
    void testCompilingModeDrawModeInFlags() {
        // Verify that the draw mode is correctly passed in flags
        final int[] capturedDrawMode = new int[1];

        TessellatorManager.setCompiling((quads, primitives, flags) -> { capturedDrawMode[0] = flags.drawMode; });

        CapturingTessellator tess = (CapturingTessellator) TessellatorManager.get();

        // Test with GL_TRIANGLES
        tess.startDrawing(GL11.GL_TRIANGLES);
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(0.5, 1, 0).tex(0.5, 1).endVertex();
        tess.draw();

        assertEquals(GL11.GL_TRIANGLES, capturedDrawMode[0], "Flags should contain GL_TRIANGLES draw mode");

        // Test with GL_QUADS
        tess.startDrawingQuads();
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(1, 1, 0).tex(1, 1).endVertex();
        tess.pos(0, 1, 0).tex(0, 1).endVertex();
        tess.draw();

        assertEquals(GL11.GL_QUADS, capturedDrawMode[0], "Flags should contain GL_QUADS draw mode");

        TessellatorManager.stopCompiling();
    }
}
