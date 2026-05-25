package com.gtnewhorizon.gtnhlib.util.font;

/**
 * Interface for mod GuiColors enums. Provides format code support from lang files via
 * {@link FontRendering#getTextPrefix(String)}. Mods implement {@link #getUnlocalized()} and get {@link #getTextPrefix()}
 * and {@link #format(String)} for free.
 */
public interface GuiColorEnum {

    String getUnlocalized();

    default String getTextPrefix() {
        return FontRendering.getTextPrefix(getUnlocalized());
    }

    default String format(String text) {
        return getTextPrefix() + text;
    }
}
