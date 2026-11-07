package com.gtnewhorizon.gtnhlib.util.numberformatting.options;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_TRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.EXPONENTIAL_FORMAT;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.util.numberformatting.ExponentialFormat;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@SuppressWarnings({ "unchecked", "unused" })
public abstract class NumberOptionsBase<T extends NumberOptionsBase<T>> {

    private int decimalPlaces = 2;

    private @NotNull BigDecimal exponentialThreshold = BD_TRILLION;
    private boolean disableExponentialFormatting = false;
    private @NotNull RoundingMode roundingMode = RoundingMode.HALF_UP;
    private @Nullable ExponentialFormat exponentialFormatOverride = null;

    protected NumberOptionsBase() {}

    // ---------- Setters ----------

    public @NotNull T setDecimalPlaces(int decimalPlaces) {
        if (decimalPlaces < 0) throw new IllegalArgumentException("Decimal places must be >= 0");
        this.decimalPlaces = decimalPlaces;
        return (T) this;
    }

    public @NotNull T setExponentialThreshold(Number number) {
        if (number == null) throw new IllegalArgumentException("Number cannot be null");
        BigDecimal value = NumberFormatUtil.bigDecimalConverter(number);
        if (value.signum() < 0) throw new IllegalArgumentException("Exponential threshold must be >= 0");
        this.exponentialThreshold = value.abs();
        return (T) this;
    }

    public @NotNull T disableExponentialFormatting() {
        this.disableExponentialFormatting = true;
        return (T) this;
    }

    public @NotNull T enableExponentialFormatting() {
        this.disableExponentialFormatting = false;
        return (T) this;
    }

    public @NotNull T setRoundingMode(@NotNull RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
        return (T) this;
    }

    public @NotNull T setExponentialFormatOverride(@Nullable ExponentialFormat override) {
        this.exponentialFormatOverride = override;
        return (T) this;
    }

    // ---------- Getters ----------

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public @NotNull BigDecimal getExponentialThreshold() {
        return exponentialThreshold;
    }

    public boolean isExponentialFormattingDisabled() {
        return disableExponentialFormatting == true;
    }

    public boolean isExponentialFormattingEnabled() {
        return disableExponentialFormatting == false;
    }

    public @NotNull RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public @NotNull ExponentialFormat getExponentialFormat() {
        return this.exponentialFormatOverride == null ? EXPONENTIAL_FORMAT : this.exponentialFormatOverride;
    }

    // ---------- Converters ----------

    public @NotNull FormatOptions toFormatOptions() {
        FormatOptions options = new FormatOptions();
        options.setExponentialThreshold(exponentialThreshold);
        options.setDecimalPlaces(decimalPlaces);
        options.setRoundingMode(roundingMode);
        options.setExponentialFormatOverride(exponentialFormatOverride);

        if (disableExponentialFormatting) {
            options.disableExponentialFormatting();
        } else {
            options.enableExponentialFormatting();
        }

        return options;
    }

    public @NotNull CompactOptions toCompactOptions() {
        CompactOptions options = new CompactOptions();
        options.setExponentialThreshold(exponentialThreshold);
        options.setDecimalPlaces(decimalPlaces);
        options.setRoundingMode(roundingMode);
        options.setExponentialFormatOverride(exponentialFormatOverride);

        if (disableExponentialFormatting) {
            options.disableExponentialFormatting();
        } else {
            options.enableExponentialFormatting();
        }

        return options;
    }
}
