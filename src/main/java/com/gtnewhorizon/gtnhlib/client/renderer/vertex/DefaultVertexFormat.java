package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.writers.ItemVBOQuadWriter;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.writers.PositionColorTextureQuadWriter;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.writers.PositionQuadWriter;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.writers.PositionTextureColorQuadWriter;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.writers.PositionTextureQuadWriter;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement.Type;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormatElement.Usage;

public class DefaultVertexFormat {

    public static final VertexFormatElement POSITION_ELEMENT = new VertexFormatElement(
            0,
            Type.FLOAT,
            Usage.POSITION,
            3);
    public static final VertexFormatElement COLOR_ELEMENT = new VertexFormatElement(0, Type.UBYTE, Usage.COLOR, 4);
    public static final VertexFormatElement TEXTURE_ELEMENT = new VertexFormatElement(0, Type.FLOAT, Usage.PRIMARY_UV, 2);
    public static final VertexFormatElement LIGHT_ELEMENT = new VertexFormatElement(1, Type.SHORT, Usage.SECONDARY_UV, 2);
    public static final VertexFormatElement NORMAL_ELEMENT = new VertexFormatElement(0, Type.BYTE, Usage.NORMAL, 3);
    public static final VertexFormatElement PADDING_ELEMENT = new VertexFormatElement(0, Type.BYTE, Usage.PADDING, 1);
    public static final VertexFormat POSITION_TEXTURE_NORMAL = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(TEXTURE_ELEMENT)
                    .add(NORMAL_ELEMENT).add(PADDING_ELEMENT).build(),
            new ItemVBOQuadWriter());
    public static final VertexFormat POSITION_COLOR_TEXTURE_LIGHT_NORMAL = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(COLOR_ELEMENT)
                    .add(TEXTURE_ELEMENT).add(LIGHT_ELEMENT).add(NORMAL_ELEMENT).add(PADDING_ELEMENT).build());
    public static final VertexFormat POSITION_TEXTURE_COLOR_LIGHT = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(TEXTURE_ELEMENT)
                    .add(COLOR_ELEMENT).add(LIGHT_ELEMENT).build());
    public static final VertexFormat POSITION = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).build(),
            new PositionQuadWriter());
    public static final VertexFormat POSITION_COLOR = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(COLOR_ELEMENT).build());
    public static final VertexFormat POSITION_COLOR_LIGHT = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(COLOR_ELEMENT).add(LIGHT_ELEMENT)
                    .build());
    public static final VertexFormat POSITION_TEXTURE = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(TEXTURE_ELEMENT).build(),
            new PositionTextureQuadWriter());
    public static final VertexFormat POSITION_COLOR_TEXTURE = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(COLOR_ELEMENT)
                    .add(TEXTURE_ELEMENT).build(),
            new PositionColorTextureQuadWriter());
    public static final VertexFormat POSITION_TEXTURE_COLOR = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(TEXTURE_ELEMENT)
                    .add(COLOR_ELEMENT).build(),
            new PositionTextureColorQuadWriter());
    public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(COLOR_ELEMENT)
                    .add(TEXTURE_ELEMENT).add(LIGHT_ELEMENT).build());
    public static final VertexFormat POSITION_TEXTURE_LIGHT_COLOR = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(TEXTURE_ELEMENT)
                    .add(LIGHT_ELEMENT).add(COLOR_ELEMENT).build());
    public static final VertexFormat POSITION_TEXTURE_COLOR_NORMAL = new VertexFormat(
            new ImmutableList.Builder<VertexFormatElement>().add(POSITION_ELEMENT).add(TEXTURE_ELEMENT)
                    .add(COLOR_ELEMENT).add(NORMAL_ELEMENT).add(PADDING_ELEMENT).build());

}
