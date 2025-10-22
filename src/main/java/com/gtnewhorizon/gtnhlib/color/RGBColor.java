package com.gtnewhorizon.gtnhlib.color;

public class RGBColor implements ImmutableColor {

    public static final ImmutableColor WHITE = new RGBColor(255, 255, 255, 255);

    public int red, green, blue, alpha;

    public RGBColor() { }

    public RGBColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = 255;
    }

    public RGBColor(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public RGBColor set(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = 255;
        return this;
    }

    public RGBColor set(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        return this;
    }

    @Override
    public int getRed() {
        return red;
    }

    @Override
    public int getGreen() {
        return green;
    }

    @Override
    public int getBlue() {
        return blue;
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    public static RGBColor fromRGB(int value) {
        return new RGBColor((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, 255);
    }

    public static RGBColor fromARGB(int value) {
        return new RGBColor((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, (value >> 24) & 0xFF);
    }

    public static RGBColor fromRGBA(int value) {
        return new RGBColor((value >> 24) & 0xFF, (value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF);
    }

    public static RGBColor fromABGR(int value) {
        return new RGBColor(value & 0xFF, (value >> 8) & 0xFF, (value >> 16) & 0xFF, (value >> 24) & 0xFF);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof RGBColor RGBColor)) return false;

        return red == RGBColor.red && green == RGBColor.green && blue == RGBColor.blue && alpha == RGBColor.alpha;
    }

    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + green;
        result = 31 * result + blue;
        result = 31 * result + alpha;
        return result;
    }

    @Override
    public String toString() {
        return "Color{" + "red=" + red + ", green=" + green + ", blue=" + blue + ", alpha=" + alpha + '}';
    }
}
