package com.gtnewhorizon.gtnhlib.util.font;

/**
 * For use with Angelica's custom fonts
 */
public interface IFontParameters {

    @Deprecated
    default float getGlyphScaleX() {
        return 1;
    }

    @Deprecated
    default float getGlyphScaleY() {
        return 1;
    }

    @Deprecated
    default float getGlyphSpacing() {
        return 0;
    }

    default float getWhitespaceScale() {
        return 1;
    }

    default float getShadowOffset() {
        return 1;
    }

    float getCharWidthFine(char chr);
}
