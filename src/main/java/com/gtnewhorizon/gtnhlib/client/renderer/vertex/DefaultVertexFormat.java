package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement.Type;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement.Usage;

import static com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFlags.*;

public final class DefaultVertexFormat {

    private DefaultVertexFormat() {
        // non-instantiable class
    }

    // --------------- ELEMENTS ---------------

    public static final VertexFormatElement POSITION_ELEMENT = new VertexFormatElement(
            0,
            Type.FLOAT,
            Usage.POSITION,
            3,
            POSITION_BIT,
            new PositionVertexAttributeWriter());
    public static final VertexFormatElement COLOR_ELEMENT = new VertexFormatElement(
            0,
            Type.UBYTE,
            Usage.COLOR,
            4,
            COLOR_BIT,
            new ColorVertexAttributeWriter());
    public static final VertexFormatElement TEXTURE_ELEMENT = new VertexFormatElement(
            0,
            Type.FLOAT,
            Usage.PRIMARY_UV,
            2,
            TEXTURE_BIT,
            new TextureVertexAttributeWriter());
    public static final VertexFormatElement LIGHT_ELEMENT = new VertexFormatElement(
            1,
            Type.SHORT,
            Usage.SECONDARY_UV,
            2,
            BRIGHTNESS_BIT,
            new LightVertexAttributeWriter());
    public static final VertexFormatElement NORMAL_ELEMENT = new VertexFormatElement(
            0,
            Type.BYTE,
            Usage.NORMAL,
            3,
            NORMAL_BIT,
            new NormalVertexAttributeWriter());
    public static final VertexFormatElement PADDING_ELEMENT = new VertexFormatElement(
            0,
            Type.BYTE,
            Usage.PADDING,
            1,
            0x0,
            null);

    // --------------- FORMATS ---------------

    public static final VertexFormat POSITION_TEXTURE_NORMAL = new VertexFormat(
            POSITION_ELEMENT,
            TEXTURE_ELEMENT,
            NORMAL_ELEMENT,
            PADDING_ELEMENT);
    public static final VertexFormat POSITION_TEXTURE_LIGHT_NORMAL = new VertexFormat(
            POSITION_ELEMENT,
            TEXTURE_ELEMENT,
            LIGHT_ELEMENT,
            NORMAL_ELEMENT,
            PADDING_ELEMENT);
    public static final VertexFormat POSITION_COLOR_TEXTURE_LIGHT_NORMAL = new VertexFormat(
            POSITION_ELEMENT,
            COLOR_ELEMENT,
            TEXTURE_ELEMENT,
            LIGHT_ELEMENT,
            NORMAL_ELEMENT,
            PADDING_ELEMENT);
    public static final VertexFormat POSITION_TEXTURE_COLOR_LIGHT = new VertexFormat(
            POSITION_ELEMENT,
            TEXTURE_ELEMENT,
            COLOR_ELEMENT,
            LIGHT_ELEMENT);
    public static final VertexFormat POSITION = new VertexFormat(POSITION_ELEMENT);
    public static final VertexFormat POSITION_COLOR = new VertexFormat(POSITION_ELEMENT, COLOR_ELEMENT);
    public static final VertexFormat POSITION_COLOR_LIGHT = new VertexFormat(
            POSITION_ELEMENT,
            COLOR_ELEMENT,
            LIGHT_ELEMENT);
    public static final VertexFormat POSITION_TEXTURE = new VertexFormat(POSITION_ELEMENT, TEXTURE_ELEMENT);
    public static final VertexFormat POSITION_TEXTURE_COLOR = new VertexFormat(
            POSITION_ELEMENT,
            TEXTURE_ELEMENT,
            COLOR_ELEMENT);
    // Duplicate of POSITION_TEXTURE_COLOR
    @Deprecated
    public static final VertexFormat POSITION_COLOR_TEXTURE = POSITION_TEXTURE_COLOR;
    public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = new VertexFormat(
            POSITION_ELEMENT,
            COLOR_ELEMENT,
            TEXTURE_ELEMENT,
            LIGHT_ELEMENT);
    public static final VertexFormat POSITION_TEXTURE_LIGHT_COLOR = new VertexFormat(
            POSITION_ELEMENT,
            TEXTURE_ELEMENT,
            LIGHT_ELEMENT,
            COLOR_ELEMENT);
    public static final VertexFormat POSITION_TEXTURE_COLOR_NORMAL = new VertexFormat(
            POSITION_ELEMENT,
            TEXTURE_ELEMENT,
            COLOR_ELEMENT,
            NORMAL_ELEMENT,
            PADDING_ELEMENT);

}
