package com.gtnewhorizon.gtnhlib.util.numberformatting.options;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_TRILLION;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@SuppressWarnings({ "unchecked", "unused" })
public abstract class NumberOptionsBase<T extends NumberOptionsBase<T>> {

    private int decimalPlaces = 2;

    private BigDecimal scientificThreshold = BD_TRILLION;
    private boolean disableScientificFormatting = false;
    private RoundingMode roundingMode = RoundingMode.HALF_UP;

    protected NumberOptionsBase() {}

    // ---------- Fluent setters ----------

    public T setDecimalPlaces(int decimalPlaces) {
        if (decimalPlaces < 0) throw new IllegalArgumentException("Decimal places must be >= 0");
        this.decimalPlaces = decimalPlaces;
        return (T) this;
    }

    public T setScientificThreshold(Number number) {
        if (number == null) throw new IllegalArgumentException("Number cannot be null");
        BigDecimal value = NumberFormatUtil.bigDecimalConverter(number);
        if (value.signum() < 0) throw new IllegalArgumentException("Scientific threshold must be >= 0");
        this.scientificThreshold = value.abs();
        return (T) this;
    }

    public T disableScientificFormatting() {
        this.disableScientificFormatting = true;
        return (T) this;
    }

    public T enableScientificFormatting() {
        this.disableScientificFormatting = false;
        return (T) this;
    }

    public T setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
        return (T) this;
    }

    // ---------- Getters ----------

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public BigDecimal getScientificThreshold() {
        return scientificThreshold;
    }

    public boolean isScientificFormattingDisabled() {
        return disableScientificFormatting == true;
    }

    public boolean isScientificFormattingEnabled() {
        return disableScientificFormatting == false;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    // ---------- Converters ----------

    public FormatOptions toFormatOptions() {
        FormatOptions options = new FormatOptions();
        options.setScientificThreshold(scientificThreshold);
        options.setDecimalPlaces(decimalPlaces);
        options.setRoundingMode(roundingMode);

        if (disableScientificFormatting) {
            options.disableScientificFormatting();
        } else {
            options.enableScientificFormatting();
        }

        return options;
    }

    public CompactOptions toCompactOptions() {
        CompactOptions options = new CompactOptions();
        options.setScientificThreshold(scientificThreshold);
        options.setDecimalPlaces(decimalPlaces);
        options.setRoundingMode(roundingMode);

        if (disableScientificFormatting) {
            options.disableScientificFormatting();
        } else {
            options.enableScientificFormatting();
        }

        return options;
    }

}
