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

    private int minDecimalPlaces = 0;
    private int maxDecimalPlaces = 2;
    private @NotNull BigDecimal exponentialThreshold = BD_TRILLION;
    private boolean disableExponentialFormatting = false;
    private @NotNull RoundingMode roundingMode = RoundingMode.HALF_UP;
    private @Nullable ExponentialFormat exponentialFormatOverride = null;

    protected NumberOptionsBase() {}

    /**
     * Creates a new format option with the value of another format option.
     * 
     * @param other The instance to copy from.
     */
    protected NumberOptionsBase(NumberOptionsBase<?> other) {
        this.minDecimalPlaces = other.minDecimalPlaces;
        this.maxDecimalPlaces = other.maxDecimalPlaces;
        this.exponentialThreshold = other.exponentialThreshold;
        this.disableExponentialFormatting = other.disableExponentialFormatting;
        this.roundingMode = other.roundingMode;
        this.exponentialFormatOverride = other.exponentialFormatOverride;
    }

    public abstract String format(Number number);

    public abstract @NotNull T copy();

    // ---------- Setters ----------

    public @NotNull T setDecimalPlaces(int decimalPlaces) {
        return this.setDecimalPlaces(0, decimalPlaces);
    }

    public @NotNull T setDecimalPlaces(int minDecimalPlaces, int maxDecimalPlaces) {
        if (minDecimalPlaces < 0) throw new IllegalArgumentException("Min decimal places must be >= 0");
        if (maxDecimalPlaces < 0) throw new IllegalArgumentException("Max decimal places must be >= 0");
        if (minDecimalPlaces > maxDecimalPlaces)
            throw new IllegalArgumentException("Min decimal places must be <= max");
        this.minDecimalPlaces = minDecimalPlaces;
        this.maxDecimalPlaces = maxDecimalPlaces;
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

    /**
     * @deprecated Use {@link #getMaxDecimalPlaces} and {@link #getMinDecimalPlaces} instead
     */
    @Deprecated
    public int getDecimalPlaces() {
        return getMaxDecimalPlaces();
    }

    public int getMaxDecimalPlaces() {
        return maxDecimalPlaces;
    }

    public int getMinDecimalPlaces() {
        return minDecimalPlaces;
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

    protected void copy(NumberOptionsBase<?> other) {
        this.exponentialThreshold = other.exponentialThreshold;
        this.minDecimalPlaces = other.minDecimalPlaces;
        this.maxDecimalPlaces = other.maxDecimalPlaces;
        this.exponentialFormatOverride = other.exponentialFormatOverride;
        this.disableExponentialFormatting = other.disableExponentialFormatting;
    }

    public @NotNull FormatOptions toFormatOptions() {
        FormatOptions options = new FormatOptions();
        options.setExponentialThreshold(exponentialThreshold);
        options.setDecimalPlaces(minDecimalPlaces, maxDecimalPlaces);
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
        options.setDecimalPlaces(minDecimalPlaces, maxDecimalPlaces);
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
