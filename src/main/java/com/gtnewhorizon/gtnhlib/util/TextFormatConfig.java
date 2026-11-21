package com.gtnewhorizon.gtnhlib.util;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = "gtnhlib", category = "text_formatting", filename = "gtnhlib-text-formatting")
public final class TextFormatConfig {

    @Config.Comment("The default length for a line separator")
    @Config.DefaultInt(41)
    @Config.RangeInt(min = 0, max = 100)
    public static int lineSeparatorDefaultLength;

    @Config.Comment("Which style should tooltip separator lines have?")
    @Config.DefaultEnum("ContinuousLine")
    public static TextFormatUtil.LineSeparatorStyle lineSeparatorStyle;

}
