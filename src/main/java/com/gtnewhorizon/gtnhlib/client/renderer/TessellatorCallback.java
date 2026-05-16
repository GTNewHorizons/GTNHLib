package com.gtnewhorizon.gtnhlib.client.renderer;

public interface TessellatorCallback {

    default boolean onStartDrawing(CallbackTessellator tessellator, int drawMode) {
        return true;
    }

    default boolean onDraw(CallbackTessellator tessellator) {
        return true;
    }

    default void onVertex(CallbackTessellator tessellator, double x, double y, double z) {
        tessellator
                .writeVertex(x + tessellator.getXOffset(), y + tessellator.getYOffset(), z + tessellator.getZOffset());
    }

    default void onTextureUV(CallbackTessellator tessellator, double u, double v) {
        tessellator.writeTextureUV(u, v);
    }

    default void onColor(CallbackTessellator tessellator, int red, int green, int blue, int alpha) {
        tessellator.writeColor(red, green, blue, alpha);
    }

    default void onNormal(CallbackTessellator tessellator, float nx, float ny, float nz) {
        tessellator.writeNormal(nx, ny, nz);
    }
}
