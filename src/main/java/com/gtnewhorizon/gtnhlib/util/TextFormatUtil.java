package com.gtnewhorizon.gtnhlib.util;

import java.util.ArrayDeque;

import net.minecraft.util.EnumChatFormatting;

public class TextFormatUtil {

    // non-customizable formats
    public static final String ITALIC = EnumChatFormatting.ITALIC.toString();

    // push/pop constants
    public static final char FORMAT_ESCAPE_PREFIX = '§';
    public static final String FORMAT_PUSH_STACK = FORMAT_ESCAPE_PREFIX + "s";
    public static final String FORMAT_POP_STACK = FORMAT_ESCAPE_PREFIX + "t";

    /**
     * Pre-processes a localized chat message with custom format push/pop instructions. This allows nested localizations
     * to set their own format without clobbering whatever came before, without needing any extra context info.
     * <p>
     * </p>
     * Example: {@code §a§lHello §s§eWorld§t!} is transformed into {@code §a§lHello §eWorld§a§l!}
     */
    public static String processFormatStacks(String message) {
        // Short circuit if there aren't any pops, because pops mutate while pushes don't.
        // Invalid codes are ignored by the font renderer so it won't cause problems if we ignore erroneous pushes.
        if (!message.contains(FORMAT_POP_STACK)) return message;

        StringBuilder out = new StringBuilder();
        out.ensureCapacity(message.length() * 6 / 5);

        int len = message.length();

        ArrayDeque<String> stack = new ArrayDeque<>();
        String currentFormat = "";

        int start = 0;

        while (start < len) {
            // Find the next format escape char
            int end = message.indexOf(FORMAT_ESCAPE_PREFIX, start);

            // If we hit the end of the string, push the rest of the input and stop
            if (end == -1) {
                out.append(message, start, len);
                break;
            }

            // If there was any text from the end of the previous code to the start of this one, add it to the output
            // buffer
            if (end > start) {
                out.append(message, start, end);
            }

            // If the current format escape char has a code after it, check it
            if (end < len - 1) {
                char code = message.charAt(end + 1);

                if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f') {
                    // Colours, use as-is and erase the previous format
                    currentFormat = "" + FORMAT_ESCAPE_PREFIX + code;
                    out.append(FORMAT_ESCAPE_PREFIX).append(code);
                } else if (code >= 'k' && code <= 'o') {
                    // Styles, use as-is and append to the colour style (note: this may act up with repeated style
                    // codes but it shouldn't break anything).
                    currentFormat = currentFormat + FORMAT_ESCAPE_PREFIX + code;
                    out.append(FORMAT_ESCAPE_PREFIX).append(code);
                } else if (code == 'r') {
                    // Reset, use as-is and clear the format
                    currentFormat = "";
                    out.append(FORMAT_ESCAPE_PREFIX).append(code);
                } else if (code == 's') {
                    // Push, save the current format and don't emit to the output buffer
                    stack.push(currentFormat.isEmpty() ? "§r" : currentFormat);
                } else if (code == 't') {
                    // Pop, restore the top format and don't emit to the output buffer
                    currentFormat = stack.isEmpty() ? "§r" : stack.pop();
                    out.append(currentFormat);
                }

                // Skip the format escape along with its code
                start = end + 2;
            } else {
                // if there is a lone § at the end abort and don't append the format char.
                break;
            }
        }

        // A format code at the very end has nothing left to format, so a pop that lands there only emits noise.
        // Drop it, otherwise consumers that match whole lines (tooltip separators, for one) never see a clean line.
        while (out.length() >= 2 && out.charAt(out.length() - 2) == FORMAT_ESCAPE_PREFIX) {
            out.setLength(out.length() - 2);
        }

        return out.toString();
    }

    /**
     * Isolates any formatting within the given text, returning to the original formatting once the text is rendered.
     *
     * @param message The text to format.
     * @return The formatted string
     */
    public static String wrapStack(String message) {
        return FORMAT_PUSH_STACK + message + FORMAT_POP_STACK;
    }

    /**
     * Applies a formatting to the given text and isolates any changes to the message itself, returning to the original
     * formatting once the text is rendered.
     *
     * @param formatting The formatting codes to apply to the text.
     * @param text       The text to format.
     * @return The formatted string
     */
    public static String wrapFormatting(String formatting, String text) {
        return FORMAT_PUSH_STACK + formatting + text + FORMAT_POP_STACK;
    }

    /**
     * Applies a formatting to the given text and isolates any changes to the message itself, returning to the original
     * formatting once the text is rendered.
     *
     * @param formatting The formatting codes to apply to the text.
     * @param text       The text to format.
     * @return The formatted string
     */
    public static String wrapFormatting(EnumChatFormatting formatting, String text) {
        // duplicated code to reduce stack pressure
        return FORMAT_PUSH_STACK + formatting + text + FORMAT_POP_STACK;
    }

}
