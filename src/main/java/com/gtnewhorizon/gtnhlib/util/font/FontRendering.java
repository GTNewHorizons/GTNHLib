package com.gtnewhorizon.gtnhlib.util.font;

import java.util.function.Function;

import net.minecraft.client.gui.FontRenderer;

import lombok.Setter;

// Common font rendering utilities that may be better-behaved than vanilla counterparts
public class FontRendering {

    private static final char FORMATTING_CHAR = 167; // §

    @Setter
    private static Function<String, String> textPreprocessor = null;

    /**
     * Extension of {@link Function} that lets a preprocessor declare capability metadata. Width calculations
     * ({@link #sizeStringToWidth(String,int,FontRenderer)},
     * {@link #trimStringToWidth(String,int,boolean,FontRenderer)}) consult {@link #handlesAmpCodes()} to decide whether
     * {@code &}-prefixed sequences should be treated as zero-width. A plain {@link Function} preprocessor is still
     * accepted by {@link #setTextPreprocessor(Function)}; it defaults to {@code handlesAmpCodes() == false}, so
     * {@code &} is counted as a literal character.
     */
    public interface TextPreprocessor extends Function<String, String> {

        /**
         * Whether this preprocessor converts ampersand color codes (&c, &#RRGGBB, &g gradients) into their §-prefixed
         * equivalents. Evaluated per call, so implementations may return a live value tied to configuration state.
         */
        default boolean handlesAmpCodes() {
            return false;
        }
    }

    private static boolean preprocessorHandlesAmpCodes() {
        return textPreprocessor instanceof TextPreprocessor && ((TextPreprocessor) textPreprocessor).handlesAmpCodes();
    }

    /**
     * Apply the registered text preprocessor (e.g. &RRGGBB → §x conversion). Returns the input unchanged if no
     * preprocessor is set.
     */
    public static String preprocessText(String str) {
        return textPreprocessor != null ? textPreprocessor.apply(str) : str;
    }

    /**
     * Count visible characters in a string after preprocessing (e.g. &RRGGBB → §x conversion), skipping all §-prefixed
     * formatting pairs.
     */
    public static int countVisibleChars(String str) {
        if (str == null || str.isEmpty()) return 0;
        str = preprocessText(str);
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == FORMATTING_CHAR && i + 1 < str.length()) {
                i++;
            } else {
                count++;
            }
        }
        return count;
    }

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

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isHex6(String str, int start) {
        if (start + 6 > str.length()) return false;
        for (int k = 0; k < 6; k++) {
            if (!isHexChar(str.charAt(start + k))) return false;
        }
        return true;
    }

    private static boolean isValidAmpCode(char c) {
        char cl = Character.toLowerCase(c);
        return (cl >= '0' && cl <= '9') || (cl >= 'a' && cl <= 'f')
                || (cl >= 'k' && cl <= 'o')
                || cl == 'r'
                || cl == 'x'
                || cl == 'q'
                || cl == 'z'
                || cl == 'v';
        // Note: 'g' excluded — &g only valid as part of &g&#RRGGBB&#RRGGBB (handled by isAmpGradient)
    }

    /**
     * Check if there is a valid &g&#RRGGBB&#RRGGBB gradient sequence starting at the given index. The char at pos must
     * be '&' and the char at pos+1 must be 'g'.
     */
    private static boolean isAmpGradient(String str, int pos) {
        // &g&#RRGGBB&#RRGGBB = 18 chars total
        if (pos + 18 > str.length()) return false;
        return str.charAt(pos + 2) == '&' && str.charAt(pos + 3) == '#'
                && isHex6(str, pos + 4)
                && str.charAt(pos + 10) == '&'
                && str.charAt(pos + 11) == '#'
                && isHex6(str, pos + 12);
    }

    /**
     * A getStringWidth implementation that respects formatting rules and works with custom fonts through Angelica.
     */
    public static int getStringWidth(String str, FontRenderer fr) {

        if (str == null || str.isEmpty()) {
            return 0;
        }

        str = preprocessText(str);

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
                width += 1.0f;
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
                case '&':
                    // Skip & color codes as zero-width when the preprocessor declares it converts them to § at
                    // render time.
                    if (preprocessorHandlesAmpCodes() && i + 1 < originalStringLength) {
                        char next = str.charAt(i + 1);
                        if (next == '#' && isHex6(str, i + 2)) {
                            // &#RRGGBB (8 chars)
                            i += 7;
                            break;
                        } else if (Character.toLowerCase(next) == 'g' && isAmpGradient(str, i)) {
                            // &g&#RRGGBB&#RRGGBB (18 chars)
                            i += 17;
                            break;
                        } else if (isValidAmpCode(next)) {
                            // &X single code (2 chars)
                            curBold = determineIfBold(curBold, next);
                            i += 1;
                            break;
                        }
                    }
                    // Literal & — fall through to default width calculation
                    // (fall through)
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
                        width += 1.0f;
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
     * Removes characters from the string to trim its rendered length down to trimWidth, starting either from the end
     * (reverse = 0) or the beginning (reverse = 1).
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
            } else if (charWidth < 0) {
                parsingFormatCode = true;
            } else if (!reverse && ch == '&' && preprocessorHandlesAmpCodes() && i + 1 < str.length()) {
                // Skip & color codes as zero-width (forward direction only) when the preprocessor declares it
                // converts them to § at render time.
                char next = str.charAt(i + 1);
                int skip = 0;
                if (next == '#' && isHex6(str, i + 2)) {
                    skip = 7; // &#RRGGBB (8 chars total, 7 after &)
                } else if (Character.toLowerCase(next) == 'g' && isAmpGradient(str, i)) {
                    skip = 17; // &g&#RRGGBB&#RRGGBB (18 chars total)
                } else if (isValidAmpCode(next)) {
                    skip = 1; // &X (2 chars total)
                    curBold = determineIfBold(curBold, next);
                }
                if (skip > 0) {
                    // Append all & code chars to output without counting width
                    for (int k = 0; k <= skip; k++) {
                        stringbuilder.append(str.charAt(i + k));
                    }
                    i += skip;
                    continue;
                }
                // Literal & — fall through to width calculation below
                width += charWidth;

                if (spacingOmittedOnce) {
                    width += fontParams.getGlyphSpacing();
                } else {
                    spacingOmittedOnce = true;
                }

                if (curBold) {
                    width += 1.0f;
                }
            } else {
                width += charWidth;

                // Add glyph spacing (n-1) times for n characters
                if (spacingOmittedOnce) {
                    width += fontParams.getGlyphSpacing();
                } else {
                    spacingOmittedOnce = true;
                }

                if (curBold) {
                    width += 1.0f;
                }
            }

            if (Math.ceil(width) > trimWidth) {
                break;
            }

            if (reverse) {
                stringbuilder.insert(0, ch);
            } else {
                stringbuilder.append(ch);
            }
        }

        return stringbuilder.toString();
    }
}
