package com.gtnewhorizon.gtnhlib.util.parsing;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jetbrains.annotations.NotNull;

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
    /**
     * the number of visible fraction digits in N, with trailing zeros.
     */
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

    public CLDRPluralRuleNumber(@NotNull BigDecimal n, @NotNull BigInteger i, int v, int w, @NotNull BigInteger f,
            @NotNull BigInteger t, int c) {
        this.n = n;
        this.i = i;
        this.v = v;
        this.w = w;
        this.f = f;
        this.t = t;
        this.c = c;
    }
}
