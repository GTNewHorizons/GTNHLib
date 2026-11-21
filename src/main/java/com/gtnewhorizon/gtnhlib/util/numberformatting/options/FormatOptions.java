package com.gtnewhorizon.gtnhlib.util.numberformatting.options;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@SuppressWarnings("unused")
public final class FormatOptions extends NumberOptionsBase<FormatOptions> {

    public FormatOptions() {}

    /**
     * Creates a new format option with the value of another format option.
     * 
     * @param other The instance to copy from.
     */
    public FormatOptions(FormatOptions other) {
        super(other);
    }

    @Override
    public String format(Number number) {
        return NumberFormatUtil.formatNumber(number, this);
    }

    @Override
    public @NotNull FormatOptions copy() {
        return new FormatOptions(this);
    }
}
