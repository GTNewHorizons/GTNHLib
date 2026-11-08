package com.gtnewhorizon.gtnhlib.util.numberformatting.options;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@SuppressWarnings("unused")
public final class FormatOptions extends NumberOptionsBase<FormatOptions> {

    public FormatOptions() {}

    @Override
    public String format(Number number) {
        return NumberFormatUtil.formatNumber(number, this);
    }

    @Override
    public @NotNull FormatOptions copy() {
        return this.toFormatOptions();
    }
}
