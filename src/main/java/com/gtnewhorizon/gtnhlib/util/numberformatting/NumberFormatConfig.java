package com.gtnewhorizon.gtnhlib.util.numberformatting;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "gtnhlib", category = "number_formatting", filename = "gtnhlib-number-formatting")
public final class NumberFormatConfig {

    @Config.Comment({ "Completely disables ALL number formatting, all numbers will show 'raw', i.e. 1000000", })
    @Config.DefaultBoolean(false)
    public static boolean disableFormattedNotation = false;

    @Config.Comment({ "Automatically switch to scientific notation above this absolute value.", })
    @Config.RangeDouble(min = 0)
    @Config.DefaultDouble(1_000_000_000_000.0) // 1 Trillion
    public static double scientificThreshold = 1_000_000_000_000.0;

    @Config.Comment({ "Number of decimal places used for normal number formatting." })
    @Config.RangeInt(min = 0)
    @Config.DefaultInt(2)
    public static int decimalPlaces = 2;

    @Config.Comment({ "Number of decimal places used for scientific notation." })
    @Config.RangeInt(min = 1)
    @Config.DefaultInt(2)
    public static int scientificDecimalPlaces = 2;

    @Config.Comment({ "True indicates to use mB, false uses L." })
    @Config.DefaultBoolean(true)
    public static boolean useForgeFluidMillibuckets = true;
}
