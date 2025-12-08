package com.gtnewhorizon.gtnhlib.client.renderer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.line.ModelLine;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.primitive.ModelPrimitiveView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.tri.ModelTriangle;

/**
 * Unit tests for PrimitiveExtractor - tests extraction of lines, triangles, quads from tessellator buffers.
 */
public class PrimitiveExtractorTest {

    @Test
    void testLineExtraction() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw lines
        tess.startDrawing(GL11.GL_LINES);
        tess.pos(0, 0, 0).color(255, 0, 0, 255).endVertex();
        tess.pos(1, 1, 1).color(255, 0, 0, 255).endVertex();
        tess.pos(2, 0, 0).color(0, 255, 0, 255).endVertex();
        tess.pos(3, 1, 1).color(0, 255, 0, 255).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(2, primitives.size(), "Should have extracted 2 lines");

        // Verify first line
        ModelPrimitiveView line1 = primitives.get(0);
        assertTrue(line1 instanceof ModelLine, "First primitive should be ModelLine");
        assertEquals(2, line1.getVertexCount(), "Line should have 2 vertices");
        assertEquals(0.0f, line1.getX(0), 0.001f, "Line1 start X");
        assertEquals(1.0f, line1.getX(1), 0.001f, "Line1 end X");

        // Verify second line
        ModelPrimitiveView line2 = primitives.get(1);
        assertTrue(line2 instanceof ModelLine, "Second primitive should be ModelLine");
        assertEquals(2.0f, line2.getX(0), 0.001f, "Line2 start X");
        assertEquals(3.0f, line2.getX(1), 0.001f, "Line2 end X");

        tess.clearPrimitives();
    }

    @Test
    void testTriangleExtraction() {
        CapturingTessellator tess = new CapturingTessellator();

        tess.startDrawing(GL11.GL_TRIANGLES);
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(0.5f, 1, 0).tex(0.5f, 1).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(1, primitives.size(), "Should have extracted 1 triangle");

        ModelPrimitiveView tri = primitives.get(0);
        assertTrue(tri instanceof ModelTriangle, "Primitive should be ModelTriangle");
        assertEquals(3, tri.getVertexCount(), "Triangle should have 3 vertices");
        assertEquals(0.0f, tri.getX(0), 0.001f, "Triangle v0 X");
        assertEquals(1.0f, tri.getX(1), 0.001f, "Triangle v1 X");
        assertEquals(0.5f, tri.getX(2), 0.001f, "Triangle v2 X");

        tess.clearPrimitives();
    }

    @Test
    void testQuadExtraction() {
        CapturingTessellator tess = new CapturingTessellator();

        tess.startDrawingQuads();
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(1, 1, 0).tex(1, 1).endVertex();
        tess.pos(0, 1, 0).tex(0, 1).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(1, primitives.size(), "Should have extracted 1 quad");

        ModelPrimitiveView quad = primitives.get(0);
        assertTrue(quad instanceof ModelQuad, "Primitive should be ModelQuad");
        assertEquals(4, quad.getVertexCount(), "Quad should have 4 vertices");

        tess.clearPrimitives();
    }

    @Test
    void testMixedPrimitiveExtraction() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw quads
        tess.startDrawingQuads();
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(1, 1, 0).tex(1, 1).endVertex();
        tess.pos(0, 1, 0).tex(0, 1).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        // End drawing state and draw lines
        tess.isDrawing = false;
        tess.reset();
        tess.startDrawing(GL11.GL_LINES);
        tess.pos(2, 0, 0).color(255, 0, 0, 255).endVertex();
        tess.pos(3, 1, 1).color(255, 0, 0, 255).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(2, primitives.size(), "Should have 2 primitives (1 quad + 1 line)");

        long quads = primitives.stream().filter(p -> p instanceof ModelQuad).count();
        long lines = primitives.stream().filter(p -> p instanceof ModelLine).count();
        assertEquals(1, quads, "Should have 1 quad");
        assertEquals(1, lines, "Should have 1 line");

        tess.clearPrimitives();
    }

    @Test
    void testLineStripExtraction() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw line strip: 4 vertices → 3 line segments
        tess.startDrawing(GL11.GL_LINE_STRIP);
        tess.pos(0, 0, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(1, 0, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(1, 1, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(0, 1, 0).color(255, 255, 255, 255).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(3, primitives.size(), "LINE_STRIP with 4 vertices should produce 3 lines");

        for (ModelPrimitiveView prim : primitives) {
            assertTrue(prim instanceof ModelLine, "All primitives should be ModelLine");
        }

        tess.clearPrimitives();
    }

    @Test
    void testLineLoopExtraction() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw line loop: 4 vertices → 4 line segments (including closing segment)
        tess.startDrawing(GL11.GL_LINE_LOOP);
        tess.pos(0, 0, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(1, 0, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(1, 1, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(0, 1, 0).color(255, 255, 255, 255).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(4, primitives.size(), "LINE_LOOP with 4 vertices should produce 4 lines");

        // Verify closing segment connects last to first
        ModelLine closingLine = (ModelLine) primitives.get(3);
        assertEquals(0.0f, closingLine.getX(0), 0.001f, "Closing line start should be at last vertex X");
        assertEquals(1.0f, closingLine.getY(0), 0.001f, "Closing line start should be at last vertex Y");
        assertEquals(0.0f, closingLine.getX(1), 0.001f, "Closing line end should be at first vertex X");
        assertEquals(0.0f, closingLine.getY(1), 0.001f, "Closing line end should be at first vertex Y");

        tess.clearPrimitives();
    }

    @Test
    void testTriangleStripExtraction() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw triangle strip: 4 vertices → 2 triangles
        tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(0, 1, 0).tex(0, 1).endVertex();
        tess.pos(1, 1, 0).tex(1, 1).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(2, primitives.size(), "TRIANGLE_STRIP with 4 vertices should produce 2 triangles");

        for (ModelPrimitiveView prim : primitives) {
            assertTrue(prim instanceof ModelTriangle, "All primitives should be ModelTriangle");
            assertEquals(3, prim.getVertexCount());
        }

        tess.clearPrimitives();
    }

    @Test
    void testTriangleFanExtraction() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw triangle fan: center + 4 outer vertices → 3 triangles
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tess.pos(0.5f, 0.5f, 0).tex(0.5f, 0.5f).endVertex(); // center
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(1, 1, 0).tex(1, 1).endVertex();
        tess.pos(0, 1, 0).tex(0, 1).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(3, primitives.size(), "TRIANGLE_FAN with 5 vertices should produce 3 triangles");

        // All triangles should share vertex 0 (the center)
        for (ModelPrimitiveView prim : primitives) {
            assertTrue(prim instanceof ModelTriangle, "All primitives should be ModelTriangle");
            ModelTriangle tri = (ModelTriangle) prim;
            assertEquals(0.5f, tri.getX(0), 0.001f, "All triangles should share center vertex X");
            assertEquals(0.5f, tri.getY(0), 0.001f, "All triangles should share center vertex Y");
        }

        tess.clearPrimitives();
    }

    @Test
    void testFlagsDrawModeTracking() {
        CapturingTessellator tess = new CapturingTessellator();

        tess.startDrawing(GL11.GL_LINES);
        tess.pos(0, 0, 0).endVertex();
        tess.pos(1, 1, 1).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        assertEquals(GL11.GL_LINES, tess.flags.drawMode, "Flags should track GL_LINES drawMode");

        tess.clearPrimitives();
    }

    @Test
    void testModelLineFullAttributes() {
        // Test that ModelLine properly stores and retrieves all vertex attributes
        CapturingTessellator tess = new CapturingTessellator();

        tess.startDrawing(GL11.GL_LINES);
        tess.pos(0, 0, 0).tex(0.25f, 0.75f).color(255, 128, 64, 200).brightness(0xF000F0).normal(0, 1, 0).endVertex();
        tess.pos(1, 1, 1).tex(0.5f, 0.5f).color(100, 150, 200, 255).brightness(0xA000A0).normal(1, 0, 0).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        ModelLine line = (ModelLine) tess.getPrimitives().get(0);

        assertEquals(2, line.getVertexCount(), "Line should have 2 vertices");

        // Verify first vertex
        assertEquals(0.0f, line.getX(0), 0.001f);
        assertEquals(0.25f, line.getTexU(0), 0.001f, "Line should store texU");
        assertEquals(0.75f, line.getTexV(0), 0.001f, "Line should store texV");
        assertEquals(0xF000F0, line.getLight(0), "Line should store brightness");
        assertNotEquals(0, line.getForgeNormal(0), "Line should store normal");

        // Verify second vertex
        assertEquals(1.0f, line.getX(1), 0.001f);
        assertEquals(0.5f, line.getTexU(1), 0.001f);
        assertEquals(0.5f, line.getTexV(1), 0.001f);

        tess.clearPrimitives();
    }

    @Test
    void testModelTriangleNormalComputation() {
        ModelTriangle tri = new ModelTriangle();

        // Create a triangle in XY plane facing +Z
        tri.setX(0, 0);
        tri.setY(0, 0);
        tri.setZ(0, 0);
        tri.setX(1, 1);
        tri.setY(1, 0);
        tri.setZ(1, 0);
        tri.setX(2, 0);
        tri.setY(2, 1);
        tri.setZ(2, 0);

        int normal = tri.getComputedFaceNormal();
        assertNotEquals(0, normal, "Computed normal should not be zero");
    }

    @Test
    void testFlagsEquality() {
        CapturingTessellator.Flags flags1 = new CapturingTessellator.Flags(true, true, true, true);
        flags1.drawMode = GL11.GL_QUADS;

        CapturingTessellator.Flags flags2 = new CapturingTessellator.Flags(true, true, true, true);
        flags2.drawMode = GL11.GL_LINES;

        assertNotEquals(flags1, flags2, "Flags with different drawMode should not be equal");

        flags2.drawMode = GL11.GL_QUADS;
        assertEquals(flags1, flags2, "Flags with same drawMode should be equal");
    }

    @Test
    void testQuadsToTrianglesConversion() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw a quad: v0(0,0), v1(1,0), v2(1,1), v3(0,1)
        tess.startDrawingQuads();
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(1, 1, 0).tex(1, 1).endVertex();
        tess.pos(0, 1, 0).tex(0, 1).endVertex();

        // Pass convertQuadsToTriangles=true
        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags,
                true); // convertQuadsToTriangles

        List<ModelPrimitiveView> primitives = tess.getPrimitives();

        // Should have 2 triangles, not 1 quad
        assertEquals(2, primitives.size(), "Quad should be converted to 2 triangles");
        assertTrue(primitives.get(0) instanceof ModelTriangle, "First primitive should be triangle");
        assertTrue(primitives.get(1) instanceof ModelTriangle, "Second primitive should be triangle");

        // Flags should indicate GL_TRIANGLES
        assertEquals(GL11.GL_TRIANGLES, tess.flags.drawMode, "drawMode should be GL_TRIANGLES after conversion");

        // Triangle 1: v0, v1, v2 -> (0,0), (1,0), (1,1)
        ModelTriangle tri1 = (ModelTriangle) primitives.get(0);
        assertEquals(0.0f, tri1.getX(0), 0.001f);
        assertEquals(0.0f, tri1.getY(0), 0.001f);
        assertEquals(1.0f, tri1.getX(1), 0.001f);
        assertEquals(0.0f, tri1.getY(1), 0.001f);
        assertEquals(1.0f, tri1.getX(2), 0.001f);
        assertEquals(1.0f, tri1.getY(2), 0.001f);

        // Triangle 2: v0, v2, v3 -> (0,0), (1,1), (0,1)
        ModelTriangle tri2 = (ModelTriangle) primitives.get(1);
        assertEquals(0.0f, tri2.getX(0), 0.001f);
        assertEquals(0.0f, tri2.getY(0), 0.001f);
        assertEquals(1.0f, tri2.getX(1), 0.001f);
        assertEquals(1.0f, tri2.getY(1), 0.001f);
        assertEquals(0.0f, tri2.getX(2), 0.001f);
        assertEquals(1.0f, tri2.getY(2), 0.001f);

        tess.clearPrimitives();
    }

    @Test
    void testDegeneratePrimitiveFiltering() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw a degenerate line (both vertices at same position)
        tess.startDrawing(GL11.GL_LINES);
        tess.pos(5, 5, 5).color(255, 0, 0, 255).endVertex();
        tess.pos(5, 5, 5).color(255, 0, 0, 255).endVertex(); // Same as first vertex
        // Draw a valid line
        tess.pos(0, 0, 0).color(0, 255, 0, 255).endVertex();
        tess.pos(1, 1, 1).color(0, 255, 0, 255).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        // Degenerate line should be filtered out, only valid line remains
        assertEquals(1, primitives.size(), "Degenerate line should be filtered out");

        ModelLine validLine = (ModelLine) primitives.get(0);
        assertEquals(0.0f, validLine.getX(0), 0.001f, "Valid line should start at origin");

        tess.clearPrimitives();
    }

    @Test
    void testDegenerateTriangleFiltering() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw a degenerate triangle (all vertices at same position)
        tess.startDrawing(GL11.GL_TRIANGLES);
        tess.pos(3, 3, 3).tex(0, 0).endVertex();
        tess.pos(3, 3, 3).tex(0, 0).endVertex();
        tess.pos(3, 3, 3).tex(0, 0).endVertex();
        // Draw a valid triangle
        tess.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.pos(1, 0, 0).tex(1, 0).endVertex();
        tess.pos(0.5f, 1, 0).tex(0.5f, 1).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(1, primitives.size(), "Degenerate triangle should be filtered out");

        tess.clearPrimitives();
    }

    @Test
    void testLineOffsetApplication() {
        CapturingTessellator tess = new CapturingTessellator();

        tess.startDrawing(GL11.GL_LINES);
        tess.pos(0, 0, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(1, 1, 1).color(255, 255, 255, 255).endVertex();

        // Apply offset of (10, 20, 30)
        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                10,
                20,
                30,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(1, primitives.size());

        ModelLine line = (ModelLine) primitives.get(0);
        // Vertex 0: (0,0,0) + (10,20,30) = (10,20,30)
        assertEquals(10.0f, line.getX(0), 0.001f, "Line start X should have offset applied");
        assertEquals(20.0f, line.getY(0), 0.001f, "Line start Y should have offset applied");
        assertEquals(30.0f, line.getZ(0), 0.001f, "Line start Z should have offset applied");
        // Vertex 1: (1,1,1) + (10,20,30) = (11,21,31)
        assertEquals(11.0f, line.getX(1), 0.001f, "Line end X should have offset applied");
        assertEquals(21.0f, line.getY(1), 0.001f, "Line end Y should have offset applied");
        assertEquals(31.0f, line.getZ(1), 0.001f, "Line end Z should have offset applied");

        tess.clearPrimitives();
    }

    @Test
    void testLineStripOffsetApplication() {
        CapturingTessellator tess = new CapturingTessellator();

        tess.startDrawing(GL11.GL_LINE_STRIP);
        tess.pos(0, 0, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(1, 0, 0).color(255, 255, 255, 255).endVertex();
        tess.pos(1, 1, 0).color(255, 255, 255, 255).endVertex();

        // Apply offset of (5, 5, 5)
        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                5,
                5,
                5,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        List<ModelPrimitiveView> primitives = tess.getPrimitives();
        assertEquals(2, primitives.size(), "LINE_STRIP with 3 vertices should produce 2 lines");

        // First line: (0,0,0)-(1,0,0) + offset = (5,5,5)-(6,5,5)
        ModelLine line1 = (ModelLine) primitives.get(0);
        assertEquals(5.0f, line1.getX(0), 0.001f);
        assertEquals(6.0f, line1.getX(1), 0.001f);

        // Second line: (1,0,0)-(1,1,0) + offset = (6,5,5)-(6,6,5)
        ModelLine line2 = (ModelLine) primitives.get(1);
        assertEquals(6.0f, line2.getX(0), 0.001f);
        assertEquals(6.0f, line2.getX(1), 0.001f);
        assertEquals(6.0f, line2.getY(1), 0.001f);

        tess.clearPrimitives();
    }

    @Test
    void testLineStripPreservesFullAttributes() {
        CapturingTessellator tess = new CapturingTessellator();

        // Draw line strip with full attributes
        tess.startDrawing(GL11.GL_LINE_STRIP);
        tess.pos(0, 0, 0).tex(0.25f, 0.5f).color(255, 128, 64, 200).brightness(0xF000F0).normal(0, 1, 0).endVertex();
        tess.pos(1, 1, 1).tex(0.75f, 0.25f).color(100, 150, 200, 255).brightness(0xA000A0).normal(1, 0, 0).endVertex();

        PrimitiveExtractor.buildPrimitivesFromBuffer(
                tess.rawBuffer,
                tess.vertexCount,
                tess.drawMode,
                tess.hasTexture,
                tess.hasBrightness,
                tess.hasColor,
                tess.hasNormals,
                0,
                0,
                0,
                -1,
                tess.quadPool,
                tess.triPool,
                tess.linePool,
                tess.collectedPrimitives,
                tess.flags);

        ModelLine line = (ModelLine) tess.getPrimitives().get(0);

        // Verify all attributes were preserved from first vertex
        assertEquals(0.25f, line.getTexU(0), 0.001f, "Line strip should preserve texU");
        assertEquals(0.5f, line.getTexV(0), 0.001f, "Line strip should preserve texV");
        assertEquals(0xF000F0, line.getLight(0), "Line strip should preserve brightness");
        assertNotEquals(0, line.getForgeNormal(0), "Line strip should preserve normal");

        // Verify second vertex attributes
        assertEquals(0.75f, line.getTexU(1), 0.001f, "Line strip should preserve texU for vertex 1");
        assertEquals(0.25f, line.getTexV(1), 0.001f, "Line strip should preserve texV for vertex 1");

        tess.clearPrimitives();
    }

    @Test
    void testIsEmptyPrimitiveUtility() {
        // Test the shared utility directly
        int[] buffer = new int[16]; // 2 vertices * 8 ints

        // All zeros - degenerate
        assertTrue(PrimitiveExtractor.isEmptyPrimitive(buffer, 0, 2), "All-zero vertices should be empty");

        // Different X values - not degenerate
        buffer[0] = Float.floatToRawIntBits(0.0f);
        buffer[8] = Float.floatToRawIntBits(1.0f); // Second vertex X is different
        assertFalse(PrimitiveExtractor.isEmptyPrimitive(buffer, 0, 2), "Different X should not be empty");

        // Reset and test Y difference
        buffer[0] = 0;
        buffer[8] = 0;
        buffer[1] = Float.floatToRawIntBits(0.0f);
        buffer[9] = Float.floatToRawIntBits(1.0f); // Second vertex Y is different
        assertFalse(PrimitiveExtractor.isEmptyPrimitive(buffer, 0, 2), "Different Y should not be empty");
    }
}
