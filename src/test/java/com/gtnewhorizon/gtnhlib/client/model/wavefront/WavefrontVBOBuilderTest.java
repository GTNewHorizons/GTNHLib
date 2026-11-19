package com.gtnewhorizon.gtnhlib.client.model.wavefront;

import static com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities.*;
import static com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.client.model.ModelFormatException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement.Type;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement.Usage;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers.IVertexAttributeWriter;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers.PositionVertexAttributeWriter;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.writers.TextureVertexAttributeWriter;

public class WavefrontVBOBuilderTest {

    private static final int A_POSITION = 3;
    private static final int A_UV = 7;

    private static final VertexFormat GENERIC_POSITION_UV = new VertexFormat(
            new VertexFormatElement(
                    A_POSITION,
                    Type.FLOAT,
                    Usage.GENERIC,
                    3,
                    POSITION_BIT,
                    new PositionVertexAttributeWriter()),
            new VertexFormatElement(
                    A_UV,
                    Type.FLOAT,
                    Usage.GENERIC,
                    2,
                    TEXTURE_BIT,
                    new TextureVertexAttributeWriter()));

    private static final String TRIANGLE_OBJ = "v 0 0 0\n" + "v 1 0 0\n"
            + "v 0 1 0\n"
            + "vt 0 0\n"
            + "vt 1 0\n"
            + "vt 0 1\n"
            + "f 1/1 2/2 3/3\n";

    private static final String QUAD_OBJ = "v 0 0 0\n" + "v 1 0 0\n"
            + "v 1 1 0\n"
            + "v 0 1 0\n"
            + "vt 0 0\n"
            + "vt 1 0\n"
            + "vt 1 1\n"
            + "vt 0 1\n"
            + "f 1/1 2/2 3/3 4/4\n";

    private static final float UV_DELTA = 0.001f;

    /** A 12-byte float3 that is not a position, to prove the guard looks at more than the element's shape. */
    private static final IVertexAttributeWriter NOT_POSITION = new IVertexAttributeWriter() {

        @Override
        public int writeAttribute(long pointer, int[] data, int index) {
            return 12;
        }

        @Override
        public int writeAttribute(long pointer, Tessellator tessellator) {
            return 12;
        }

        @Override
        public int readAttribute(long pointer, Tessellator tessellator) {
            return 12;
        }
    };

    private final List<DirectTessellator> allocated = new ArrayList<>();

    @AfterEach
    void freeTessellators() {
        allocated.forEach(DirectTessellator::delete);
        allocated.clear();
    }

    private static InputStream obj(String source) {
        return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
    }

    private DirectTessellator tessellator(VertexFormat format) {
        final DirectTessellator tess = new DirectTessellator(1024);
        allocated.add(tess);
        if (format != null) tess.setVertexFormat(format);
        return tess;
    }

    private DirectTessellator build(String source, VertexFormat parseFormat, VertexFormat predefinedFormat) {
        final DirectTessellator tess = tessellator(predefinedFormat);
        WavefrontVBOBuilder.loadObjModel(obj(source), tess, parseFormat);
        return tess;
    }

    @Test
    void testGenericFormatIsHonoured() {
        assertEquals(20, GENERIC_POSITION_UV.getVertexSize(), "3 floats of position + 2 floats of UV");

        final DirectTessellator tess = build(TRIANGLE_OBJ, GENERIC_POSITION_UV, GENERIC_POSITION_UV);

        assertSame(GENERIC_POSITION_UV, tess.getVertexFormat(), "The format passed in must be the one used");
        assertEquals(3, tess.getVertexCount());
        assertEquals(60, tess.getWriteBuffer().limit(), "3 vertices at the custom 20-byte stride");

        final long base = memAddress0(tess.getWriteBuffer());

        assertEquals(0f, memGetFloat(base), 0.0001f);
        assertEquals(0f, memGetFloat(base + 4), 0.0001f);
        assertEquals(0f, memGetFloat(base + 8), 0.0001f);
        assertEquals(0f, memGetFloat(base + 12), UV_DELTA);
        assertEquals(1f, memGetFloat(base + 16), UV_DELTA);

        assertEquals(1f, memGetFloat(base + 20), 0.0001f);
        assertEquals(0f, memGetFloat(base + 24), 0.0001f);
        assertEquals(1f, memGetFloat(base + 32), UV_DELTA);
        assertEquals(1f, memGetFloat(base + 36), UV_DELTA);

        assertEquals(0f, memGetFloat(base + 40), 0.0001f);
        assertEquals(1f, memGetFloat(base + 44), 0.0001f);
        assertEquals(0f, memGetFloat(base + 52), UV_DELTA);
        assertEquals(0f, memGetFloat(base + 56), UV_DELTA);
    }

    @Test
    void testGenericAttributeLocationsSurvive() {
        final VertexFormat used = build(TRIANGLE_OBJ, GENERIC_POSITION_UV, GENERIC_POSITION_UV).getVertexFormat();

        assertEquals(A_POSITION, used.elementsArray[0].getIndex());
        assertEquals(A_UV, used.elementsArray[1].getIndex());
        assertEquals(Usage.GENERIC, used.elementsArray[1].getUsage());
    }

    @Test
    void testDerivedFormatCannotExpressGenericAttributes() {
        final VertexFormat derived = build(TRIANGLE_OBJ, GENERIC_POSITION_UV, null).getVertexFormat();

        assertSame(DefaultVertexFormat.POSITION_TEXTURE, derived);
        assertEquals(GENERIC_POSITION_UV.getVertexSize(), derived.getVertexSize(), "Same stride, different bindings");
        assertEquals(Usage.PRIMARY_UV, derived.elementsArray[1].getUsage(), "Binds through glTexCoordPointer");
    }

    @Test
    void testDefaultOverloadIsUnchanged() {
        final VertexFormat format = DefaultVertexFormat.POSITION_TEXTURE_NORMAL;

        final DirectTessellator derived = build(TRIANGLE_OBJ, format, null);
        final DirectTessellator predefined = build(TRIANGLE_OBJ, format, format);

        assertSame(format, derived.getVertexFormat(), "The derived format was already POSITION_TEXTURE_NORMAL");
        assertSame(format, predefined.getVertexFormat());

        final ByteBuffer a = derived.getWriteBuffer();
        final ByteBuffer b = predefined.getWriteBuffer();

        assertEquals(72, a.limit(), "3 vertices at the 24-byte POSITION_TEXTURE_NORMAL stride");
        assertEquals(a.limit(), b.limit());
        for (int i = 0; i < a.limit(); i++) {
            assertEquals(a.get(i), b.get(i), "Byte " + i + " differs from the pre-fix output");
        }
    }

    @Test
    void testQuadsKeepCustomStride() {
        final DirectTessellator tess = build(QUAD_OBJ, GENERIC_POSITION_UV, GENERIC_POSITION_UV);

        assertEquals(GL11.GL_QUADS, tess.getDrawMode());
        assertEquals(4, tess.getVertexCount());
        assertEquals(80, tess.getWriteBuffer().limit());
    }

    @Test
    void testUnfillableAttributeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> WavefrontVBOBuilder.validateFormat(DefaultVertexFormat.POSITION_TEXTURE_COLOR),
                "An OBJ cannot supply a color channel");
        assertThrows(
                IllegalArgumentException.class,
                () -> WavefrontVBOBuilder.validateFormat(DefaultVertexFormat.POSITION_TEXTURE_LIGHT),
                "An OBJ cannot supply a brightness channel");
        assertDoesNotThrow(() -> WavefrontVBOBuilder.validateFormat(DefaultVertexFormat.POSITION_TEXTURE_NORMAL));
        assertDoesNotThrow(() -> WavefrontVBOBuilder.validateFormat(GENERIC_POSITION_UV));
    }

    @Test
    void testFormatNotStartingWithPositionIsRejected() {
        final VertexFormat uvFirst = new VertexFormat(
                DefaultVertexFormat.TEXTURE_ELEMENT,
                DefaultVertexFormat.POSITION_ELEMENT);

        assertThrows(IllegalArgumentException.class, () -> WavefrontVBOBuilder.validateFormat(uvFirst));
        assertThrows(
                IllegalArgumentException.class,
                () -> tessellator(uvFirst),
                "setVertexFormat guards the same invariant for every predefined-format caller");
    }

    @Test
    void testTexturedFormatAgainstUntexturedModel() {
        final String positionsOnly = "v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n";

        assertThrows(
                ModelFormatException.class,
                () -> build(positionsOnly, DefaultVertexFormat.POSITION_TEXTURE, DefaultVertexFormat.POSITION_TEXTURE),
                "Must report the mismatch rather than dereferencing the unfilled UV array");
    }

    @Test
    void testModelWithoutFaces() {
        assertThrows(
                ModelFormatException.class,
                () -> build("v 0 0 0\nv 1 0 0\n", GENERIC_POSITION_UV, GENERIC_POSITION_UV));
    }

    @Test
    void testMalformedFaceStillPropagates() {
        final String badIndex = "v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 99\n";

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> build(badIndex, DefaultVertexFormat.POSITION, DefaultVertexFormat.POSITION));
    }

    @Test
    void testFloat3NonPositionFirstElementIsRejected() {
        final VertexFormat normalFirst = new VertexFormat(
                new VertexFormatElement(0, Type.FLOAT, Usage.GENERIC, 3, NORMAL_BIT, NOT_POSITION),
                new VertexFormatElement(
                        A_UV,
                        Type.FLOAT,
                        Usage.GENERIC,
                        2,
                        TEXTURE_BIT,
                        new TextureVertexAttributeWriter()));

        assertFalse(
                normalFirst.hasInlinePosition(),
                "Element 0 is written as inline xyz, so a float3 that is not a position must not claim that slot");
        assertThrows(IllegalArgumentException.class, () -> WavefrontVBOBuilder.validateFormat(normalFirst));
        assertThrows(IllegalArgumentException.class, () -> tessellator(normalFirst));
    }

    @Test
    void testWhitespaceOnlyLinesAreIgnored() {
        final String padded = "v 0 0 0\n   \n" + "v 1 0 0\n\t\n"
                + "v 0 1 0\n"
                + "vt 0 0\n"
                + "vt 1 0\n"
                + "vt 0 1\n"
                + "   \n"
                + "f 1/1 2/2 3/3\n";

        final ByteBuffer clean = build(TRIANGLE_OBJ, GENERIC_POSITION_UV, GENERIC_POSITION_UV).getWriteBuffer();
        final ByteBuffer spaced = build(padded, GENERIC_POSITION_UV, GENERIC_POSITION_UV).getWriteBuffer();

        assertEquals(clean.limit(), spaced.limit());
        for (int i = 0; i < clean.limit(); i++) {
            assertEquals(clean.get(i), spaced.get(i), "Byte " + i + " differs from the unpadded model");
        }
    }

    @Test
    void testMixedFaceSizesRejected() {
        final String mixed = "v 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\nf 1 2 3\nf 1 2 3 4\n";

        assertThrows(
                ModelFormatException.class,
                () -> build(mixed, DefaultVertexFormat.POSITION, DefaultVertexFormat.POSITION),
                "A single VBO carries one draw mode, so the quad cannot follow the triangle");
    }

    @Test
    void testMixedFaceFormatsRejected() {
        final String mixed = "v 0 0 0\nv 1 0 0\n" + "v 0 1 0\n"
                + "vt 0 0\n"
                + "vt 1 0\n"
                + "vt 0 1\n"
                + "f 1/1 2/2 3/3\n"
                + "f 1 2 3\n";

        assertThrows(
                ModelFormatException.class,
                () -> build(mixed, GENERIC_POSITION_UV, GENERIC_POSITION_UV),
                "The second face has no UV indices; reading stale subtokens would silently corrupt the UVs");
    }
}
