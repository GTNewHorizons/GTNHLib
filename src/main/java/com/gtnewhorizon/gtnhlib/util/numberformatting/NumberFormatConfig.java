package com.gtnewhorizon.gtnhlib.util.numberformatting;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "gtnhlib", category = "number_formatting", filename = "gtnhlib-number-formatting")
public final class NumberFormatConfig {

    @Config.Comment({ "Completely disables ALL number formatting, all numbers will show 'raw', i.e. 1000000", })
    @Config.DefaultBoolean(false)
    public static boolean disableFormattedNotation = false;

    @Config.Comment({
            "Completely disables exponential notation. No scientific, standard form or engineering notation." })
    @Config.DefaultBoolean(false)
    public static boolean globalDisableExponentialNotation = false;

    @Config.Comment({ "True indicates to use mB, false uses GregTech L." })
    @Config.DefaultBoolean(true)
    public static boolean useForgeFluidMillibuckets = true;

    @Config.Comment({ "Controls how very large numbers are rendered when scientific notation is used.",
            "  SCIENTIFIC   – Standard scientific notation (e.g. 1.23e12)",
            "  ENGINEERING  – Engineering notation with exponent in multiples of 3 (e.g. 123e9)",
            "  POWER_OF_TEN – Power-of-ten notation using explicit multiplication (e.g. 1.23*10^12)",
            "Invalid values will cause the game to fail fast with an error." })
    @Config.DefaultString("SCIENTIFIC")
    public static String formatPattern = "SCIENTIFIC";

    @Config.Ignore
    public static ExponentialFormat EXPONENTIAL_FORMAT = ExponentialFormat.SCIENTIFIC;
}
