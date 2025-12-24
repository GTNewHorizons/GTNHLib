package com.gtnewhorizon.gtnhlib.util.numberformatting;

import java.math.BigInteger;

/**
 * Optional tuning parameters for number formatting.
 *
 * <p>
 * These options adjust precision for lossy representations
 * (abbreviated and scientific notation).
 * Plain locale formatting is never rounded.
 */
public final class NumberFormatOptions {

    /**
     * Number of significant digits to use for abbreviated and scientific
     * representations.
     *
     * <p>
     * If {@code null}, a formatter-defined default is used.
     */
    private Integer significantDigits;

    /**
     * Minimum absolute value at which abbreviated formatting begins.
     *
     * <p>
     * Only consulted by compact / abbreviated formatting paths.
     * If {@code null}, a formatter-defined default is used.
     */
    private BigInteger abbreviationThreshold;

    public NumberFormatOptions() {}

    /* ========================= Precision ========================= */

    public NumberFormatOptions significantDigits(int significantDigits) {
        if (significantDigits <= 0) {
            throw new IllegalArgumentException(
                "significantDigits must be > 0");
        }
        this.significantDigits = significantDigits;
        return this;
    }

    /* ========================= Abbreviation ========================= */

    public NumberFormatOptions abbreviationThreshold(
        BigInteger abbreviationThreshold
    ) {
        if (abbreviationThreshold == null
            || abbreviationThreshold.signum() <= 0) {
            throw new IllegalArgumentException(
                "abbreviationThreshold must be > 0");
        }
        this.abbreviationThreshold = abbreviationThreshold;
        return this;
    }

    public NumberFormatOptions abbreviationThreshold(long threshold) {
        return abbreviationThreshold(BigInteger.valueOf(threshold));
    }

    /* ========================= Accessors ========================= */

    /**
     * Returns the requested significant digit count, or {@code null}
     * if the default should be used.
     */
    public Integer getSignificantDigits() {
        return significantDigits;
    }

    /**
     * Returns the abbreviation threshold, or {@code null}
     * if the default should be used.
     */
    public BigInteger getAbbreviationThreshold() {
        return abbreviationThreshold;
    }
}
