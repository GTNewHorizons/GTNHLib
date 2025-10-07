package com.gtnewhorizon.gtnhlib.client.renderer.quad;

public class TessFlags {

    boolean hasTexture;
    public boolean hasBrightness;
    public boolean hasColor;
    public boolean hasNormals;

    public TessFlags(boolean hasTexture, boolean hasBrightness, boolean hasColor, boolean hasNormals) {
        this.hasTexture = hasTexture;
        this.hasBrightness = hasBrightness;
        this.hasColor = hasColor;
        this.hasNormals = hasNormals;
    }

}
