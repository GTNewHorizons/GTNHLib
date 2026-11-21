package com.gtnewhorizon.gtnhlib.util.parsing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;

import it.unimi.dsi.fastutil.objects.ObjectIntPair;

public class CLDRPluralRuleNumber {
    // var names based on https://unicode.org/reports/tr35/tr35-numbers.html#Operands

    /**
     * the absolute value of {@see number}
     */
    public @NotNull final BigDecimal n;
    /**
     * the integer digits of {@see number}
     */
    public @NotNull final BigInteger i;
    /// The number of visible fraction digits in N, with trailing zeros.
    /// Set to -1 when a special value is detected, else never negative.
    public final int v;
    /**
     * the number of visible fraction digits in N, without trailing zeros.*
     */
    public final int w;
    /**
     * the visible fraction digits in N, with trailing zeros, expressed as an integer.*
     */
    public @NotNull final BigInteger f;
    /**
     * the visible fraction digits in N, without trailing zeros, expressed as an integer.*
     */
    public @NotNull final BigInteger t;
    /**
     * compact decimal exponent value: exponent of the power of 10 used in compact decimal formatting.
     */
    public final int c;

    @VisibleForTesting
    public CLDRPluralRuleNumber(@NotNull BigDecimal n, @NotNull BigInteger i, int v, int w, @NotNull BigInteger f,
            @NotNull BigInteger t, int c) {
        this.n = n.stripTrailingZeros();
        this.i = i;
        this.v = v;
        this.w = w;
        this.f = f;
        this.t = t;
        this.c = c;
    }

    public CLDRPluralRuleNumber(@Nullable Number value, @NotNull NumberOptionsBase<?> options) {
        boolean isSpecialCase = value == null;
        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            isSpecialCase = Double.isInfinite(d) || Double.isNaN(d);
        }

        if (isSpecialCase) {
            this.n = BigDecimal.ZERO;
            this.i = BigInteger.ZERO;
            this.v = -1;
            this.w = 0;
            this.f = BigInteger.ZERO;
            this.t = BigInteger.ZERO;
            this.c = 0;
            return;
        }

        BigDecimal orig = NumberFormatUtil.bigDecimalConverter(value);
        BigDecimal val = orig.abs();
        BigDecimal stripped;
        boolean fixExponentialValue;
        if (options instanceof CompactOptions co && val.compareTo(co.getCompactThreshold()) >= 0) {
            var ret = NumberFormatUtil.getAbbreviatedNumberInformation(val, options);
            if (ret != null) {
                stripped = ret.rounded.abs();
                c = ret.exp;
                fixExponentialValue = true;
            } else {
                stripped = val.stripTrailingZeros();
                c = 0;
                fixExponentialValue = false;
            }
        } else if (NumberFormatUtil.isExponentialFormattingEnabled(val, options)) {
            ObjectIntPair<BigDecimal> ret = options.getExponentialFormat().getComponents(orig, options);
            stripped = ret.left().abs();
            c = ret.rightInt();
            fixExponentialValue = true;
        } else {
            stripped = val.stripTrailingZeros();
            c = 0;
            fixExponentialValue = false;
        }

        if (fixExponentialValue) {
            // the mantissa will always be stripped so we just need to check if it has enough decimals
            if (options.getMinDecimalPlaces() > stripped.scale()) {
                // add missing zeros if the scale of the mantissa is too low.
                val = stripped.setScale(options.getMinDecimalPlaces(), RoundingMode.UNNECESSARY);
                // offset values by exponent
                val = val.scaleByPowerOfTen(c);
                stripped = stripped.scaleByPowerOfTen(c);
            } else {
                // offset values by exponent
                val = stripped = stripped.scaleByPowerOfTen(c);
            }
        } else {
            // round if needed
            if (options.getMaxDecimalPlaces() < stripped.scale()) {
                val = stripped.setScale(options.getMaxDecimalPlaces(), options.getRoundingMode());
                stripped = val.stripTrailingZeros();
            }

            // add missing zeros if stripped number has too few digits
            if (options.getMinDecimalPlaces() > stripped.scale()) {
                val = stripped.setScale(options.getMinDecimalPlaces(), RoundingMode.UNNECESSARY);
            }
            // strip extra zeros above min digits if needed
            else if (options.getMinDecimalPlaces() < val.scale()) {
                int scaleBy = Math.max(options.getMinDecimalPlaces(), stripped.scale());
                val = stripped.setScale(scaleBy, RoundingMode.UNNECESSARY);
            }
        }

        // Below threshold, do exact formatting

        n = stripped;
        i = n.toBigInteger();
        v = Math.max(0, val.scale());
        w = Math.max(0, stripped.scale());
        f = val.remainder(BigDecimal.ONE).movePointRight(v).toBigInteger();
        t = stripped.remainder(BigDecimal.ONE).movePointRight(w).toBigInteger();

    }
}
