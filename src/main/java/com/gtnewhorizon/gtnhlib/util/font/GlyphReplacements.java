package com.gtnewhorizon.gtnhlib.util.font;

import java.util.HashMap;

import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;

public final class GlyphReplacements {

    /**
     * A mapping from custom glyphs to the values they are meant to replace (which may be null for novel glyphs or when
     * the original comes from outside the BMP).
     */
    @Deprecated
    public static final HashMap<String, String> customGlyphs = new HashMap<>(64);

    private static final Char2CharOpenHashMap customGlyphsNew = new Char2CharOpenHashMap();

    @Deprecated // Use registerCustomGlyph(String replacement, char original) instead
    public static String registerCustomGlyph(String replacement, String original) {
        customGlyphs.put(replacement, original);
        customGlyphsNew.put(replacement.charAt(0), original.charAt(0));
        return replacement;
    }

    @Deprecated // Does nothing, can be removed.
    public static String registerCustomGlyph(String replacement) {
        return replacement;
    }

    public static String registerCustomGlyph(String replacement, char original) {
        customGlyphsNew.put(replacement.charAt(0), original);
        return replacement;
    }

    public static char getReplacementGlyph(char ch) {
        return customGlyphsNew.get(ch);
    }
}
