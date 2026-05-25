package com.gtnewhorizon.gtnhlib.util.font;

import net.minecraft.client.gui.FontRenderer;

/**
 * Interface for mod GuiColors enums. Provides format code support from lang files via
 * {@link FontRendering#getTextPrefix(String)}. Mods implement {@link #getUnlocalized()} and {@link #getColor()}, then
 * get {@link #format(String)} and {@link #drawString(FontRenderer, String, int, int)} for free.
 */
public interface GuiColorEnum {

    String getUnlocalized();

    int getColor();

    default String getTextPrefix() {
        return FontRendering.getTextPrefix(getUnlocalized());
    }

    default String format(String text) {
        return getTextPrefix() + text;
    }

    default int drawString(FontRenderer fr, String text, int x, int y) {
        return fr.drawString(format(text), x, y, getColor());
    }
}
