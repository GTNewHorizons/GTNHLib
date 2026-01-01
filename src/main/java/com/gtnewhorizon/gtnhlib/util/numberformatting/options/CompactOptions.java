package com.gtnewhorizon.gtnhlib.util.numberformatting.options;

import java.math.BigDecimal;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

@SuppressWarnings("unchecked")
public final class CompactOptions extends NumberOptionsBase<CompactOptions> {

    private static final BigDecimal DEFAULT_COMPACT_THRESHOLD = BigDecimal.valueOf(1_000);
    private BigDecimal compactThreshold;

    public CompactOptions() {}

    // ---------- Setters ----------

    @Override
    public CompactOptions setExponentialThreshold(Number number) {
        BigDecimal value = NumberFormatUtil.bigDecimalConverter(number);
        if (value.signum() < 0) throw new IllegalArgumentException("Scientific threshold must be >= 0");

        // Prevent conflict with compact threshold.
        // Compact must *always* be lower than the scientific threshold, or configuration becomes inconsistent.
        BigDecimal currentCompact = getCompactThreshold();
        if (value.compareTo(currentCompact) <= 0) {
            throw new IllegalArgumentException(
                    "Scientific threshold must be strictly greater than compact threshold (" + currentCompact + ")");
        }

        super.setExponentialThreshold(value);
        return this;
    }

    public CompactOptions setCompactThreshold(Number number) {
        BigDecimal value = NumberFormatUtil.bigDecimalConverter(number);
        if (value.signum() < 0) throw new IllegalArgumentException("Compact threshold must be >= 0");

        // Prevent conflict with scientific threshold.
        // Compact must *always* be lower than the scientific threshold, or configuration becomes inconsistent.
        BigDecimal currentScientific = getExponentialThreshold();
        if (value.compareTo(currentScientific) >= 0) {
            throw new IllegalArgumentException(
                    "Compact threshold must be strictly less than scientific threshold (" + currentScientific + ")");
        }

        this.compactThreshold = value;
        return this;
    }

    // ---------- Getters ----------

    public BigDecimal getCompactThreshold() {
        return (compactThreshold == null) ? DEFAULT_COMPACT_THRESHOLD : compactThreshold;
    }
}
