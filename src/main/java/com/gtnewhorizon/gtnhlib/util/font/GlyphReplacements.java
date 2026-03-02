package com.gtnewhorizon.gtnhlib.util.font;

import java.util.HashMap;

public class GlyphReplacements {

    /**
     * A mapping from custom glyphs to the values they are meant to replace (which may be null for novel glyphs or when
     * the original comes from outside the BMP).
     */
    public static final HashMap<String, String> customGlyphs = new HashMap<>(64);

    public static String registerCustomGlyph(String replacement, String original) {
        customGlyphs.put(replacement, original);
        return replacement;
    }

    public static String registerCustomGlyph(String replacement) {
        customGlyphs.put(replacement, null);
        return replacement;
    }
}
