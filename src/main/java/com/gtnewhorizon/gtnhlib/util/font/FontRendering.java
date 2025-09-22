package com.gtnewhorizon.gtnhlib.util.font;

import net.minecraft.client.gui.FontRenderer;

// Common font rendering utilities that may be better-behaved than vanilla counterparts
public class FontRendering {

    private static final char FORMATTING_CHAR = 167; // §

    public static boolean charInRange(char what, char fromInclusive, char toInclusive) {
        return (what >= fromInclusive) && (what <= toInclusive);
    }

    private static boolean determineIfBold(boolean wasBold, char fmtChar) {
        char c = Character.toLowerCase(fmtChar);
        if (c == 'l') {
            return true;
        } else {
            final boolean is09 = charInRange(c, '0', '9');
            final boolean isAF = charInRange(c, 'a', 'f');
            if (c == 'r' || is09 || isAF) {
                return false;
            }
        }
        return wasBold;
    }

    /**
     * A getStringWidth implementation that respects formatting rules and works with custom fonts through Angelica.
     */
    public static int getStringWidth(String str, FontRenderer fr) {

        if (str == null || str.isEmpty()) { return 0; }

        IFontParameters fontParams = (IFontParameters) fr;

        float width = 0;
        boolean curBold = false;
        boolean spacingOmittedOnce = false;

        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            float charWidth = fontParams.getCharWidthFine(ch);

            if (charWidth < 0 && (i + 1) < str.length()) {
                i++;
                char fmtChar = str.charAt(i);
                curBold = determineIfBold(curBold, fmtChar);
                charWidth = 0;
            }

            width += charWidth;

            // Add glyph spacing (n-1) times for n characters
            if (charWidth > 0) {
                if (spacingOmittedOnce) {
                    width += fontParams.getGlyphSpacing();
                } else {
                    spacingOmittedOnce = true;
                }
            }

            if (curBold && charWidth > 0) {
                width += fontParams.getShadowOffset();
            }
        }

        return (int) Math.ceil(width);
    }

    /**
     * Determines how many characters from the string will fit into the specified width.
     */
    public static int sizeStringToWidth(String str, int wrapWidth, FontRenderer fr) {
        int originalStringLength = str.length();
        float width = 0;
        int i = 0;
        int lastBreakSpot = -1;
        boolean curBold = false;
        boolean spacingOmittedOnce = false;

        IFontParameters fontParams = (IFontParameters) fr;

        while (i < originalStringLength) {
            char currentChar = str.charAt(i);

            if (currentChar == '\n') {
                lastBreakSpot = i;
                break;
            }

            switch (currentChar) {
                case FORMATTING_CHAR:
                    if ((i + 1) < originalStringLength) {
                        i++;
                        char fmtChar = str.charAt(i);
                        curBold = determineIfBold(curBold, fmtChar);
                    }
                    break;
                case ' ':
                    lastBreakSpot = i;
                default:
                    width += fontParams.getCharWidthFine(currentChar);

                    // Add glyph spacing (n-1) times for n characters
                    if (spacingOmittedOnce) {
                        width += fontParams.getGlyphSpacing();
                    } else {
                        spacingOmittedOnce = true;
                    }

                    if (curBold) {
                        width += fontParams.getShadowOffset();
                    }
            }

            if (Math.ceil(width) > wrapWidth) {
                break;
            }
            i++;
        }

        if (i != originalStringLength && lastBreakSpot != -1 && lastBreakSpot < i) {
            return lastBreakSpot;
        }
        return i;
    }

    /**
     * Removes characters from the string to trim its rendered length down to trimWidth,
     * starting from either the end (reverse = 0) or the beginning (reverse = 1).
     */
    public static String trimStringToWidth(String str, int trimWidth, boolean reverse, FontRenderer fr) {
        StringBuilder stringbuilder = new StringBuilder();
        float width = 0;
        int startOrEnd = reverse ? str.length() - 1 : 0;
        int increment = reverse ? -1 : 1;
        boolean curBold = false;
        boolean spacingOmittedOnce = false;
        boolean parsingFormatCode = false;

        IFontParameters fontParams = (IFontParameters) fr;

        for (int i = startOrEnd; i >= 0 && i < str.length() && width < trimWidth; i += increment) {
            char ch = str.charAt(i);
            float charWidth = fontParams.getCharWidthFine(ch);

            if (parsingFormatCode) {
                parsingFormatCode = false;
                curBold = determineIfBold(curBold, ch);
            }
            else if (charWidth < 0) {
                parsingFormatCode = true;
            }
            else {
                width += charWidth;

                // Add glyph spacing (n-1) times for n characters
                if (spacingOmittedOnce) {
                    width += fontParams.getGlyphSpacing();
                } else {
                    spacingOmittedOnce = true;
                }

                if (curBold) {
                    width += fontParams.getShadowOffset();
                }
            }

            if (Math.ceil(width) > trimWidth) {
                break;
            }

            if (reverse) {
                stringbuilder.insert(0, ch);
            }
            else {
                stringbuilder.append(ch);
            }
        }

        return stringbuilder.toString();
    }
}
