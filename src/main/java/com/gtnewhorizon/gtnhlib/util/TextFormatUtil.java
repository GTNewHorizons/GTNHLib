package com.gtnewhorizon.gtnhlib.util;

import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.templatedtext.command.DynamicFormatCommand;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class TextFormatUtil {

    public static final TooltipDescriptionFormatContainer TOOLTIPS = new TooltipDescriptionFormatContainer(
            new ResourceLocation(GTNHLib.MODID, "tooltip_descriptions.cfg"));

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

    public static class TooltipDescriptionFormatContainer extends TextThemingGuidelines {

        /** The default formatting for tooltip description text. */
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String $default = EnumChatFormatting.GRAY.toString();

        public @NotNull String getDefault() {
            return this.$default;
        }

        /** The formatting applied to the unit of certain measurements like EU/t. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String unit = EnumChatFormatting.WHITE.toString();

        /**
         * The formatting applied to the names of major components like tired multiblock components, tool names, and
         * other proper names that refer to something worth highlighting.
         */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String name = EnumChatFormatting.WHITE.toString();

        /**
         * The formatting applied to the names and amounts of things that are generally related to steam.<br>
         * Usually used along the hot and cold formats.
         */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String steam = EnumChatFormatting.WHITE.toString();

        /**
         * The formatting applied to the equation part of a formula. The term being defined is not included in this
         * eg:<br>
         * example usage: {color:name}{color_formula: = ax+b}
         */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String formula = EnumChatFormatting.WHITE.toString();

        /** The formatting applied to warning. Also used to highlight an explained parameter in formulas. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String note = EnumChatFormatting.YELLOW.toString();

        /** The formatting applied to machine type descriptions. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String machineType = EnumChatFormatting.YELLOW.toString();

        /** The formatting applied to the names of miscellaneous components or tag lines. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String misc = EnumChatFormatting.BLUE.toString();

        /**
         * The formatting applied to the names and amounts of things that are generally cold.<br>
         * Usually used along the hot and steam formats.
         */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String cold = EnumChatFormatting.BLUE.toString();

        /** The formatting applied to processing speed modifiers. For numeric values, it is applied to "x" and "%". */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String speed = EnumChatFormatting.GREEN.toString();

        /** The formatting applied to numeric values representing a machine's effective range. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String range = EnumChatFormatting.GREEN.toString();

        /** The formatting applied to numeric values representing amounts of something. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String amount = EnumChatFormatting.GREEN.toString();

        /** The formatting applied to hatch usage notices for multiblock machines. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String hatchNotice = EnumChatFormatting.GREEN.toString();

        /** The formatting applied to efficiency loss modifiers. For numeric values, it is applied to "x" and "%". */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String loss = EnumChatFormatting.GREEN.toString();

        /** The formatting applied to values representing the number of parallel recipes a machine can handle. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String parallel = EnumChatFormatting.GOLD.toString();

        /** The formatting applied to machine mode names. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String mode = EnumChatFormatting.GOLD.toString();

        /** The formatting applied to the name of components that are key to the function of what is being described. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String key = EnumChatFormatting.GOLD.toString();

        /** The formatting applied to power efficiency modifiers. For numeric values, it is applied to "x" and "%". */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String powerEfficiency = EnumChatFormatting.AQUA.toString();

        /** The formatting applied to heat measurements. Applied to the unit. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String heat = EnumChatFormatting.RED.toString();

        /** The formatting applied to things that are generally hot. Usually used along the cold and steam formats. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String hot = EnumChatFormatting.RED.toString();

        /** The formatting applied to electron-volt measurements. Always applied to the unit. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String electronVolt = EnumChatFormatting.RED.toString();

        /** The formatting applied to depreciation notices. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String deprecated = EnumChatFormatting.RED.toString();

        /** The formatting applied to notices of dangerous aspects of something. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String danger = EnumChatFormatting.DARK_RED.toString();

        /** The formatting applied to the name of a mechanic or a mechanical component of something. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String mechanic = EnumChatFormatting.LIGHT_PURPLE.toString();

        /**
         * The formatting applied to the numeric values of time measurements. Applied to the unit when using compact
         * notation like 5s.
         */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String time = EnumChatFormatting.LIGHT_PURPLE.toString();

        /** The formatting applied to the numeric values of pollution measurements. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String pollution = EnumChatFormatting.DARK_PURPLE.toString();

        /** The formatting applied to the value component of power throughput measurements (eg: EU/t measurements). */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String voltage = EnumChatFormatting.YELLOW.toString();

        /**
         * Formatting applied to the value component of power throughput measurements (eg: EU/t or RF/t measurements).
         */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String amperage = EnumChatFormatting.AQUA.toString();

        /** The formatting applied to the numeric component of power capacity measurements. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String powerCapacity = EnumChatFormatting.BLUE.toString();

        /** The formatting applied to numeric component of cable loss measurements. */
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private @NotNull String cableLoss = EnumChatFormatting.RED.toString();

        public TooltipDescriptionFormatContainer(@NotNull ResourceLocation @NotNull... resoruces) {
            super(resoruces);// assign based on resource
            this.registerKey("default", this::getDefault, this::set$default);
            this.registerKey("unit", this::getUnit, this::setUnit);
            this.registerKey("name", this::getName, this::setName);
            this.registerKey("steam", this::getSteam, this::setSteam);
            this.registerKey("formula", this::getFormula, this::setFormula);
            this.registerKey("note", this::getNote, this::setNote);
            this.registerKey("machine_type", this::getMachineType, this::setMachineType);
            this.registerKey("voltage", this::getVoltage, this::setVoltage);
            this.registerKey("misc", this::getMisc, this::setMisc);
            this.registerKey("cold", this::getCold, this::setCold);
            this.registerKey("power_capacity", this::getPowerCapacity, this::setPowerCapacity);
            this.registerKey("speed", this::getSpeed, this::setSpeed);
            this.registerKey("range", this::getRange, this::setRange);
            this.registerKey("amount", this::getAmount, this::setAmount);
            this.registerKey("hatch_notice", this::getHatchNotice, this::setHatchNotice);
            this.registerKey("loss", this::getLoss, this::setLoss);
            this.registerKey("parallel", this::getParallel, this::setParallel);
            this.registerKey("mode", this::getMode, this::setMode);
            this.registerKey("key", this::getKey, this::setKey);
            this.registerKey("power_efficiency", this::getPowerEfficiency, this::setPowerEfficiency);
            this.registerKey("amperage", this::getAmperage, this::setAmperage);
            this.registerKey("heat", this::getHeat, this::setHeat);
            this.registerKey("hot", this::getHot, this::setHot);
            this.registerKey("electron_volt", this::getElectronVolt, this::setElectronVolt);
            this.registerKey("deprecated", this::getDeprecated, this::setDeprecated);
            this.registerKey("cable_loss", this::getCableLoss, this::setCableLoss);
            this.registerKey("danger", this::getDanger, this::setDanger);
            this.registerKey("mechanic", this::getMechanic, this::setMechanic);
            this.registerKey("time", this::getTime, this::setTime);
            this.registerKey("pollution", this::getPollution, this::setPollution);
        }

        @Override
        protected void registerKey(String name, Supplier<@NotNull String> getter, Consumer<@NotNull String> setter) {
            super.registerKey(name, getter, setter);
            TemplatedTextLoader.TOOLTIP_DESCRIPTION.addCommand("color_" + name, new DynamicFormatCommand(getter));
        }
    }

}
