package com.gtnewhorizon.gtnhlib.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

public class TextFormatUtil {

    // push/pop constants
    public static final char FORMAT_ESCAPE_PREFIX = '§';
    public static final @NotNull String FORMAT_PUSH_STACK = FORMAT_ESCAPE_PREFIX + "s";
    public static final @NotNull String FORMAT_POP_STACK = FORMAT_ESCAPE_PREFIX + "t";

    /**
     * Pre-processes a localized chat message with custom format push/pop instructions. This allows nested localizations
     * to set their own format without clobbering whatever came before, without needing any extra context info.
     * <p>
     * </p>
     * Example: {@code §a§lHello §s§eWorld§t!} is transformed into {@code §a§lHello §eWorld§a§l!}
     */
    public static @NotNull String processFormatStacks(@NotNull String message) {
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
                // if there is a lone § at the end abort and don't append the broken format code.
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

    // region line separator

    /**
     * Appends a line separator with the user's preferred line separator style to a string builder.
     *
     * @see #processFormatStacks(String)
     *
     * @return The line separator, assume it contains format stacks.
     */
    public static String getLineSeparator() {
        return getLineSeparator(GTNHLibConfig.defaultLineSeparatorLength);
    }

    /**
     * Appends a line separator with the user's preferred line separator style to a string builder.
     *
     * @see #processFormatStacks(String)
     *
     * @param length The length of the line separator.
     * @return The line separator, assume it contains format stacks.
     */
    public static String getLineSeparator(int length) {
        return GTNHLibConfig.separatorStyle.get(length);
    }

    /**
     * Appends a line separator with the user's preferred line separator style to a string builder.
     *
     * @apiNote Assume the added text contains format stacks.
     * @see #processFormatStacks(String)
     *
     * @param sb The string builder to append the separator to.
     */
    public static void appendLineSeparator(StringBuilder sb) {
        appendLineSeparator(sb, GTNHLibConfig.defaultLineSeparatorLength);
    }

    /**
     * Appends a line separator with the user's preferred line separator style to a string builder.
     *
     * @apiNote Assume the added text contains format stacks.
     * @see #processFormatStacks(String)
     *
     * @param sb     The string builder to append the separator to.
     * @param length The length of the line separator.
     */
    public static void appendLineSeparator(StringBuilder sb, int length) {
        GTNHLibConfig.separatorStyle.append(sb, length);
    }

    public enum LineSeparatorStyle {

        Empty {

            @Override
            public String get(int length) {
                // not actually empty in case the line gets stripped if it is
                return " ";
            }

        },
        Dashes {

            @Override
            public String get(int length) {
                if (length <= 0) return Empty.get(0);
                final char[] chars = new char[length];
                Arrays.fill(chars, '-');
                return new String(chars);
            }

        },
        ContinuousLine {

            @Override
            public String get(int length) {
                // the java compiler turns this into a string builder so no need to make one.
                return FORMAT_PUSH_STACK + EnumChatFormatting.STRIKETHROUGH + Dashes.get(length) + FORMAT_POP_STACK;
            }

            @Override
            public void append(@NotNull StringBuilder sb, int length) {
                sb.append(FORMAT_PUSH_STACK).append(EnumChatFormatting.STRIKETHROUGH).append(Dashes.get(length))
                        .append(FORMAT_POP_STACK);
            }

        };

        public abstract String get(int length);

        public void append(@NotNull StringBuilder sb, int length) {
            if (length <= 0) return;
            sb.append(this.get(length));
        }
    }

    // endregion line separator

    // region themed unit formatters

    public static class UnitFormatterWithDualTheme extends NumberFormatUtil.UnitFormatter {

        private final @NotNull Supplier<String> numberThemeSupplier;

        public UnitFormatterWithDualTheme(@NotNull Supplier<@NotNull String> numberThemeSupplier,
                @NotNull Supplier<@NotNull String> formatSupplier, @NotNull Supplier<@NotNull String> unitThemeSupplier,
                @NotNull Supplier<@NotNull String> unitSupplier) {
            super(formatSupplier, () -> unitThemeSupplier.get() + unitSupplier.get());
            this.numberThemeSupplier = numberThemeSupplier;
        }

        @Override
        public @NotNull String format(@Nullable String value) {
            return TextFormatUtil.FORMAT_PUSH_STACK + super.format(this.numberThemeSupplier.get() + value)
                    + TextFormatUtil.FORMAT_POP_STACK;
        }
    }

    public static class UnitFormatterWithTheme extends NumberFormatUtil.UnitFormatter {

        private final @NotNull Supplier<String> themeSupplier;

        public UnitFormatterWithTheme(@NotNull Supplier<@NotNull String> themeSupplier,
                @NotNull Supplier<@NotNull String> formatSupplier, @NotNull Supplier<@NotNull String> unitSupplier) {
            super(formatSupplier, unitSupplier);
            this.themeSupplier = themeSupplier;
        }

        @Override
        public @NotNull String format(@Nullable String value) {
            return TextFormatUtil.FORMAT_PUSH_STACK + this.themeSupplier.get()
                    + super.format(value)
                    + TextFormatUtil.FORMAT_POP_STACK;
        }
    }

    public static class UnitFormatterWithNumberTheme extends NumberFormatUtil.UnitFormatter {

        private final @NotNull Supplier<String> numberThemeSupplier;

        public UnitFormatterWithNumberTheme(@NotNull Supplier<@NotNull String> numberThemeSupplier,
                @NotNull Supplier<@NotNull String> formatSupplier, @NotNull Supplier<@NotNull String> unitSupplier) {
            super(formatSupplier, unitSupplier);
            this.numberThemeSupplier = numberThemeSupplier;
        }

        @Override
        public @NotNull String format(@Nullable String value) {
            return super.format(
                    TextFormatUtil.FORMAT_PUSH_STACK + this.numberThemeSupplier.get()
                            + value
                            + TextFormatUtil.FORMAT_POP_STACK);
        }
    }

    public static class UnitFormatterWithUnitTheme extends NumberFormatUtil.UnitFormatter {

        public UnitFormatterWithUnitTheme(@NotNull Supplier<@NotNull String> formatSupplier,
                @NotNull Supplier<@NotNull String> unitThemeSupplier, @NotNull Supplier<@NotNull String> unitSupplier) {
            super(
                    formatSupplier,
                    () -> TextFormatUtil.FORMAT_PUSH_STACK + unitThemeSupplier.get()
                            + unitSupplier.get()
                            + TextFormatUtil.FORMAT_POP_STACK);
        }
    }

    // endregion themed unit formatters

    // region themed throughput formatters

    public static class ThroughputUnitFormatterWithDualTheme extends NumberFormatUtil.ThroughputUnitFormatter {

        private final @NotNull Supplier<String> numberThemeSupplier;

        public ThroughputUnitFormatterWithDualTheme(@NotNull Supplier<@NotNull String> numberThemeSupplier,
                @NotNull Supplier<@NotNull String> formatSupplier, @NotNull Supplier<@NotNull String> unitThemeSupplier,
                @NotNull Supplier<@NotNull String> unitFormatSupplier, @NotNull Supplier<@NotNull String> unit1Supplier,
                @NotNull Supplier<@NotNull String> unit2Supplier) {
            super(
                    formatSupplier,
                    () -> unitThemeSupplier.get() + unitFormatSupplier.get(),
                    unit1Supplier,
                    unit2Supplier);
            this.numberThemeSupplier = numberThemeSupplier;
        }

        @Override
        public @NotNull String format(@Nullable String value) {
            return TextFormatUtil.FORMAT_PUSH_STACK + super.format(this.numberThemeSupplier.get() + value)
                    + TextFormatUtil.FORMAT_POP_STACK;
        }
    }

    public static class ThroughputUnitFormatterWithTheme extends NumberFormatUtil.ThroughputUnitFormatter {

        private final @NotNull Supplier<String> numberThemeSupplier;

        public ThroughputUnitFormatterWithTheme(@NotNull Supplier<@NotNull String> numberThemeSupplier,
                @NotNull Supplier<@NotNull String> formatSupplier,
                @NotNull Supplier<@NotNull String> unitFormatSupplier, @NotNull Supplier<@NotNull String> unit1Supplier,
                @NotNull Supplier<@NotNull String> unit2Supplier) {
            super(formatSupplier, unitFormatSupplier, unit1Supplier, unit2Supplier);
            this.numberThemeSupplier = numberThemeSupplier;
        }

        @Override
        public @NotNull String format(@Nullable String value) {
            return TextFormatUtil.FORMAT_PUSH_STACK + this.numberThemeSupplier.get()
                    + super.format(value)
                    + TextFormatUtil.FORMAT_POP_STACK;
        }
    }

    public static class ThroughputUnitFormatterWithNumberTheme extends NumberFormatUtil.ThroughputUnitFormatter {

        private final @NotNull Supplier<String> numberThemeSupplier;

        public ThroughputUnitFormatterWithNumberTheme(@NotNull Supplier<@NotNull String> numberThemeSupplier,
                @NotNull Supplier<@NotNull String> formatSupplier,
                @NotNull Supplier<@NotNull String> unitFormatSupplier, @NotNull Supplier<@NotNull String> unit1Supplier,
                @NotNull Supplier<@NotNull String> unit2Supplier) {
            super(formatSupplier, unitFormatSupplier, unit1Supplier, unit2Supplier);
            this.numberThemeSupplier = numberThemeSupplier;
        }

        @Override
        public @NotNull String format(@Nullable String value) {
            return super.format(
                    TextFormatUtil.FORMAT_PUSH_STACK + this.numberThemeSupplier.get()
                            + value
                            + TextFormatUtil.FORMAT_POP_STACK);
        }
    }

    public static class ThroughputUnitFormatterWithUnitTheme extends NumberFormatUtil.ThroughputUnitFormatter {

        public ThroughputUnitFormatterWithUnitTheme(@NotNull Supplier<@NotNull String> formatSupplier,
                @NotNull Supplier<@NotNull String> unitThemeSupplier,
                @NotNull Supplier<@NotNull String> unitFormatSupplier, @NotNull Supplier<@NotNull String> unit1Supplier,
                @NotNull Supplier<@NotNull String> unit2Supplier) {
            super(
                    formatSupplier,
                    () -> TextFormatUtil.FORMAT_PUSH_STACK + unitThemeSupplier.get()
                            + unitFormatSupplier.get()
                            + TextFormatUtil.FORMAT_POP_STACK,
                    unit1Supplier,
                    unit2Supplier);
        }
    }

    // endregion themed throughput formatters
}
