package com.gtnewhorizon.gtnhlib.util.numberformatting;

import java.math.BigInteger;

public final class NumberFormatOptions {

    private final Integer significantDigits;
    private final BigInteger abbreviationThreshold;

    private NumberFormatOptions(
        Integer significantDigits,
        BigInteger abbreviationThreshold
    ) {
        this.significantDigits = significantDigits;
        this.abbreviationThreshold = abbreviationThreshold;
    }

    /* ========================= Defaults ========================= */

    public static final NumberFormatOptions DEFAULT =
        new NumberFormatOptions(null, null);

    /* ========================= Resolution ========================= */

    public int significantDigitsOr(int fallback) {
        return significantDigits != null ? significantDigits : fallback;
    }

    public BigInteger abbreviationThresholdOr(BigInteger fallback) {
        return abbreviationThreshold != null ? abbreviationThreshold : fallback;
    }

    /* ========================= Builder ========================= */

    public static Builder builder() {
        return new Builder();
    }

    public static NumberFormatOptions sig(int digits) {
        return builder().significantDigits(digits).build();
    }

    public static NumberFormatOptions abbrev(BigInteger threshold) {
        return builder().abbreviationThreshold(threshold).build();
    }

    public static NumberFormatOptions abbrev(long threshold) {
        return abbrev(BigInteger.valueOf(threshold));
    }

    public static final class Builder {
        private Integer significantDigits;
        private BigInteger abbreviationThreshold;

        public Builder significantDigits(int digits) {
            this.significantDigits = digits;
            return this;
        }

        public Builder abbreviationThreshold(BigInteger threshold) {
            this.abbreviationThreshold = threshold;
            return this;
        }

        public Builder abbreviationThreshold(long threshold) {
            return abbreviationThreshold(BigInteger.valueOf(threshold));
        }

        public NumberFormatOptions build() {
            if (significantDigits == null && abbreviationThreshold == null) {
                return DEFAULT;
            }
            return new NumberFormatOptions(significantDigits, abbreviationThreshold);
        }
    }
}
