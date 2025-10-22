package com.gtnewhorizon.gtnhlib.color;

public class HSVColor implements ImmutableColor {

    public float hue, saturation, brightness;

    public HSVColor() { }

    public HSVColor(float hue, float saturation, float brightness) {
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
    }

    @Override
    public int toIntRGB() {
        return toIntARGB() & 0xFFFFFF;
    }

    @Override
    public int toIntARGB() {
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness);
    }

    @Override
    public int toIntRGBA() {
        return toIntRGB() << 8 | 0xFF;
    }

    @Override
    public HSVColor toHSV() {
        return new HSVColor(hue, saturation, brightness);
    }

    @Override
    public int getRed() {
        return (toIntARGB() >> 16) & 0xFF;
    }

    @Override
    public int getGreen() {
        return (toIntARGB() >> 8) & 0xFF;
    }

    @Override
    public int getBlue() {
        return toIntARGB() & 0xFF;
    }

    @Override
    public int getAlpha() {
        return 255;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof HSVColor hsvColor)) return false;

        return Float.compare(hue, hsvColor.hue) == 0
            && Float.compare(saturation, hsvColor.saturation) == 0
            && Float.compare(brightness, hsvColor.brightness) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.hashCode(hue);
        result = 31 * result + Float.hashCode(saturation);
        result = 31 * result + Float.hashCode(brightness);
        return result;
    }

    @Override
    public String toString() {
        return "HSVColor{" + "hue=" + hue + ", saturation=" + saturation + ", brightness=" + brightness + '}';
    }
}
