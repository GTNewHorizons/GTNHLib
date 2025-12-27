package com.gtnewhorizon.gtnhlib.util.numberformatting;

import java.math.BigInteger;

/**
 * Optional tuning parameters for number formatting.
 * <p>
 * These options adjust precision for lossy representations (scientific/compact notation).
 */
@SuppressWarnings("unused")
public final class NumberFormatOptions {

    /**
     * Number of significant digits to use for representations.
     */
    private Integer significantDigits;
    private static final int DEFAULT_SIG_DIGITS = 3;

    /**
     * Minimum absolute value at which compact formatting begins. I.e. 1000 means anything above 1000 and below -1000
     * will be compacted.
     */
    private BigInteger abbreviationThreshold;
    private static final BigInteger DEFAULT_ABBREV_THRESHOLD = BigInteger.valueOf(1_000);

    public NumberFormatOptions() {}

    /* ========================= Precision ========================= */

    public NumberFormatOptions significantDigits(int significantDigits) {
        if (significantDigits <= 0) {
            throw new IllegalArgumentException("significantDigits must be > 0");
        }
        this.significantDigits = significantDigits;
        return this;
    }

    /* ========================= Abbreviation ========================= */

    public NumberFormatOptions abbreviationThreshold(BigInteger abbreviationThreshold) {
        if (abbreviationThreshold == null || abbreviationThreshold.signum() <= 0) {
            throw new IllegalArgumentException("abbreviationThreshold must be > 0");
        }
        this.abbreviationThreshold = abbreviationThreshold;
        return this;
    }

    public NumberFormatOptions abbreviationThreshold(long threshold) {
        return abbreviationThreshold(BigInteger.valueOf(threshold));
    }

    /* ========================= Accessors ========================= */

    /**
     * Returns the requested significant digit count.
     */
    public Integer getSignificantDigits() {
        return (significantDigits == null) ? DEFAULT_SIG_DIGITS : significantDigits;
    }

    /**
     * Returns the abbreviation threshold. Only impacts compact formatting.
     */
    public BigInteger getAbbreviationThreshold() {
        return (abbreviationThreshold == null) ? DEFAULT_ABBREV_THRESHOLD : abbreviationThreshold;
    }
}
