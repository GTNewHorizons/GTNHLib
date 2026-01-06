package com.gtnewhorizon.gtnhlib.client.renderer.vertex;

import net.minecraft.client.renderer.Tessellator;

public final class VertexFlags {

    public static final int POSITION_BIT = 0x0; // Always enabled by default
    public static final int TEXTURE_BIT = 0x1;
    public static final int COLOR_BIT = 0x2;
    public static final int NORMAL_BIT = 0x4;
    public static final int BRIGHTNESS_BIT = 0x8;

    public static final int BITS_SIZE = 0x10;

    public static int convertToFlags(boolean hasTexture, boolean hasColor, boolean hasNormal, boolean hasBrightness) {
        return (hasTexture ? TEXTURE_BIT : 0) | (hasColor ? COLOR_BIT : 0)
                | (hasNormal ? NORMAL_BIT : 0)
                | (hasBrightness ? BRIGHTNESS_BIT : 0);
    }

    public static VertexFormat getFormat(boolean hasTexture, boolean hasColor, boolean hasNormal,
            boolean hasBrightness) {
        return DefaultVertexFormat.ALL_FORMATS[convertToFlags(hasTexture, hasColor, hasNormal, hasBrightness)];
    }

    public static VertexFormat getFormat(Tessellator tessellator) {
        return DefaultVertexFormat.ALL_FORMATS[convertToFlags(
                tessellator.hasTexture,
                tessellator.hasColor,
                tessellator.hasNormals,
                tessellator.hasBrightness)];
    }

    public static VertexFormat getFormat(int flags) {
        return DefaultVertexFormat.ALL_FORMATS[flags];
    }
}
