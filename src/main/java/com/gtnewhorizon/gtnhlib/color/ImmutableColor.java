package com.gtnewhorizon.gtnhlib.color;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SuppressWarnings("unused")
public interface ImmutableColor {

    int getRed();
    int getGreen();
    int getBlue();
    int getAlpha();

    default int toIntRGB() {
        return ((getRed() & 0xFF) << 16) | ((getGreen() & 0xFF) << 8) | (getBlue() & 0xFF);
    }

    default int toIntARGB() {
        return ((getAlpha() & 0xFF) << 24) | ((getRed() & 0xFF) << 16) | ((getGreen() & 0xFF) << 8) | (getBlue() & 0xFF);
    }

    default int toIntRGBA() {
        return ((getRed() & 0xFF) << 24) | ((getGreen() & 0xFF) << 16) | ((getBlue() & 0xFF) << 8) | ((getAlpha() & 0xFF));
    }

    default int toIntABGR() {
        return (getRed() & 0xFF) | ((getGreen() & 0xFF) << 8) | ((getBlue() & 0xFF) << 16) | ((getAlpha() & 0xFF) << 24);
    }

    default RGBColor toRGB() {
        return new RGBColor(getRed(), getGreen(), getBlue(), getAlpha());
    }

    default HSVColor toHSV() {
        float[] hsv = java.awt.Color.RGBtoHSB(getRed(), getGreen(), getBlue(), null);

        return new HSVColor(hsv[0], hsv[1], hsv[2]);
    }

    @SideOnly(Side.CLIENT)
    default void makeActive() {
        GL11.glColor4f(getRed() / 256f, getGreen() / 256f, getBlue() / 256f, getAlpha() / 256f);
    }

    default short[] toShorts() {
        return new short[] { (short) getRed(), (short) getGreen(), (short) getBlue(), (short) getAlpha() };
    }

    static HSVColor lerp(ImmutableColor a, ImmutableColor b, float k) {
        HSVColor hsvA = a instanceof HSVColor hsv ? hsv : a.toHSV();
        HSVColor hsvB = b instanceof HSVColor hsv ? hsv : b.toHSV();

        return new HSVColor(
            lerp(hsvA.hue, hsvB.hue, k),
            lerp(hsvA.saturation, hsvB.saturation, k),
            lerp(hsvA.brightness, hsvB.brightness, k));
    }

    static float lerp(float a, float b, float k) {
        return a * (1 - k) + b * k;
    }
}
