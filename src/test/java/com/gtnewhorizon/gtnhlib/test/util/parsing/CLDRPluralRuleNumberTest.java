package com.gtnewhorizon.gtnhlib.test.util.parsing;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_MILLION;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.gtnewhorizon.gtnhlib.extensions.DisableFormattedNotation;
import com.gtnewhorizon.gtnhlib.extensions.ForceDisplayLocale;
import com.gtnewhorizon.gtnhlib.extensions.ForceFormatLocale;
import com.gtnewhorizon.gtnhlib.extensions.GlobalDisableExponentialNotation;
import com.gtnewhorizon.gtnhlib.util.numberformatting.ExponentialFormat;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;
import com.gtnewhorizon.gtnhlib.util.parsing.CLDRPluralRuleNumber;

@DisableFormattedNotation(false)
@GlobalDisableExponentialNotation(false)
@ForceFormatLocale("en-US")
@ForceDisplayLocale("en-US")
public class CLDRPluralRuleNumberTest extends CLDRPluralRuleTest {

    // region standard

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_zero(FormatOptions opts) {
        testNumberContainer(0, opts.setDecimalPlaces(2, 2), "0.00e0", 0, 2, 0, 0, 0, 0);
        testNumberContainer(0, opts.setDecimalPlaces(0, 2), "0e0", 0, 0, 0, 0, 0, 0);
        testNumberContainer(0, opts.setDecimalPlaces(0, 0), "0e0", 0, 0, 0, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_general_positive(FormatOptions opts) {
        testNumberContainer(1, opts, "1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(2, opts, "2e0", 2, 0, 0, 0, 0, 0);
        testNumberContainer(0.1, opts, "1e-1", 0, 1, 1, 1, 1, -1);
        testNumberContainer(0.2, opts, "2e-1", 0, 1, 1, 2, 2, -1);
        testNumberContainer(10, opts, "1e1", 10, 0, 0, 0, 0, 1);
        testNumberContainer(12, opts, "1.2e1", 12, 0, 0, 0, 0, 1);
        testNumberContainer(12.3, opts, "1.23e1", 12, 1, 1, 3, 3, 1);
        testNumberContainer(12.34, opts, "1.23e1", 12, 1, 1, 3, 3, 1);
    }

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_general_negative(FormatOptions opts) {
        testNumberContainer(-1, opts, "-1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(-2, opts, "-2e0", 2, 0, 0, 0, 0, 0);
        testNumberContainer(-0.1, opts, "-1e-1", 0, 1, 1, 1, 1, -1);
        testNumberContainer(-0.2, opts, "-2e-1", 0, 1, 1, 2, 2, -1);
        testNumberContainer(-10, opts, "-1e1", 10, 0, 0, 0, 0, 1);
        testNumberContainer(-12, opts, "-1.2e1", 12, 0, 0, 0, 0, 1);
        testNumberContainer(-12.3, opts, "-1.23e1", 12, 1, 1, 3, 3, 1);
        testNumberContainer(-12.34, opts, "-1.23e1", 12, 1, 1, 3, 3, 1);
    }

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_digitCount_rounding_minEqualToMax(FormatOptions opts) {
        opts.setDecimalPlaces(1, 1);
        testNumberContainer(12.5, opts, "1.3e1", 13, 0, 0, 0, 0, 1);
        testNumberContainer(0.995, opts, "1.0e0", 1, 1, 0, 0, 0, 0);
        testNumberContainer(1.25e-8, opts, "1.3e-8", 0, 9, 9, 13, 13, -8);
        testNumberContainer(9.95e-8, opts, "1.0e-7", 0, 8, 7, 10, 1, -7);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(12.5, opts, "1.2e1", 12, 0, 0, 0, 0, 1);
        testNumberContainer(0.995, opts, "9.9e-1", 0, 2, 2, 99, 99, -1);
        testNumberContainer(1.25e-8, opts, "1.2e-8", 0, 9, 9, 12, 12, -8);
        testNumberContainer(9.95e-8, opts, "9.9e-8", 0, 9, 9, 99, 99, -8);
    }

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_digitCount_rounding_minLessThanMax(FormatOptions opts) {
        opts.setDecimalPlaces(0, 1);
        testNumberContainer(12.5, opts, "1.3e1", 13, 0, 0, 0, 0, 1);
        testNumberContainer(0.995, opts, "1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(1.25e-8, opts, "1.3e-8", 0, 9, 9, 13, 13, -8);
        testNumberContainer(9.95e-8, opts, "1e-7", 0, 7, 7, 1, 1, -7);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(12.5, opts, "1.2e1", 12, 0, 0, 0, 0, 1);
        testNumberContainer(0.995, opts, "9.9e-1", 0, 2, 2, 99, 99, -1);
        testNumberContainer(1.25e-8, opts, "1.2e-8", 0, 9, 9, 12, 12, -8);
        testNumberContainer(9.95e-8, opts, "9.9e-8", 0, 9, 9, 99, 99, -8);
    }

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_digitCount_rounding_maxEqualTo0(FormatOptions opts) {
        opts.setDecimalPlaces(0, 0);
        testNumberContainer(1.5, opts, "2e0", 2, 0, 0, 0, 0, 0);
        testNumberContainer(0.95, opts, "1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(1.5e-8, opts, "2e-8", 0, 8, 8, 2, 2, -8);
        testNumberContainer(9.5e-8, opts, "1e-7", 0, 7, 7, 1, 1, -7);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(1.5, opts, "1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(0.95, opts, "9e-1", 0, 1, 1, 9, 9, -1);
        testNumberContainer(1.5e-8, opts, "1e-8", 0, 8, 8, 1, 1, -8);
        testNumberContainer(9.5e-8, opts, "9e-8", 0, 8, 8, 9, 9, -8);
    }

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_digitCount_minLessThanMax(FormatOptions opts) {
        opts.setDecimalPlaces(0, 1);
        testNumberContainer(0.99, opts, "9.9e-1", 0, 2, 2, 99, 99, -1);
        testNumberContainer(1e-5, opts, "1e-5", 0, 5, 5, 1, 1, -5);
    }

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_digitCount_minEqualToMax(FormatOptions opts) {
        opts.setDecimalPlaces(1, 1);
        testNumberContainer(0.9, opts, "9.0e-1", 0, 2, 1, 90, 9, -1);
        testNumberContainer(1e-5, opts, "1.0e-5", 0, 6, 5, 10, 1, -5);
    }

    @ParameterizedTest
    @MethodSource("tryAllStandardNotationOptions")
    void standard_digitCount_minCausesOverflowIntoFraction(FormatOptions opts) {
        opts.setDecimalPlaces(5, 5);
        testNumberContainer(1200, opts, "1.20000e3", 1200, 2, 0, 0, 0, 3);
        testNumberContainer(1200.50, opts, "1.20050e3", 1200, 2, 1, 50, 5, 3);
    }

    // endregion standard

    // region engineering

    @Test
    void engineering_zero() {
        FormatOptions opts = getEngOpts();
        testNumberContainer(0, opts.setDecimalPlaces(2, 2), "0.00e0", 0, 2, 0, 0, 0, 0);
        testNumberContainer(0, opts.setDecimalPlaces(0, 2), "0e0", 0, 0, 0, 0, 0, 0);
        testNumberContainer(0, opts.setDecimalPlaces(0, 0), "0e0", 0, 0, 0, 0, 0, 0);
    }

    @Test
    void engineering_general_positive() {
        FormatOptions opts = getEngOpts();
        testNumberContainer(1, opts, "1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(2, opts, "2e0", 2, 0, 0, 0, 0, 0);
        testNumberContainer(0.1, opts, "100e-3", 0, 3, 1, 100, 1, -3);
        testNumberContainer(0.2, opts, "200e-3", 0, 3, 1, 200, 2, -3);
        testNumberContainer(10, opts, "10e0", 10, 0, 0, 0, 0, 0);
        testNumberContainer(12, opts, "12e0", 12, 0, 0, 0, 0, 0);
        testNumberContainer(1e13, opts, "10e12", 1e13, 0, 0, 0, 0, 12);
        testNumberContainer(1.23e13, opts, "12.3e12", 1.23e13, 0, 0, 0, 0, 12);
        testNumberContainer(1.23e-7, opts, "123e-9", 0, 9, 9, 123, 123, -9);
    }

    @Test
    void engineering_general_negative() {
        FormatOptions opts = getEngOpts();
        testNumberContainer(-1, opts, "-1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(-2, opts, "-2e0", 2, 0, 0, 0, 0, 0);
        testNumberContainer(-0.1, opts, "-100e-3", 0, 3, 1, 100, 1, -3);
        testNumberContainer(-0.2, opts, "-200e-3", 0, 3, 1, 200, 2, -3);
        testNumberContainer(-10, opts, "-10e0", 10, 0, 0, 0, 0, 0);
        testNumberContainer(-12, opts, "-12e0", 12, 0, 0, 0, 0, 0);
        testNumberContainer(-1e13, opts, "-10e12", 1e13, 0, 0, 0, 0, 12);
        testNumberContainer(-1.23e13, opts, "-12.3e12", 1.23e13, 0, 0, 0, 0, 12);
        testNumberContainer(-1.23e-7, opts, "-123e-9", 0, 9, 9, 123, 123, -9);

    }

    @Test
    void engineering_digitCount_rounding_minEqualToMax() {
        FormatOptions opts = getEngOpts().setDecimalPlaces(1, 1);
        testNumberContainer(1.95e0, opts, "2.0e0", 2, 1, 0, 0, 0, 0);
        testNumberContainer(999.95e-3, opts, "1.0e0", 1, 1, 0, 0, 0, 0);
        testNumberContainer(19.95e-9, opts, "20.0e-9", 0, 10, 8, 200, 2, -9);
        testNumberContainer(999.95e-9, opts, "1.0e-6", 0, 7, 6, 10, 1, -6);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(1.95e0, opts, "1.9e0", 1, 1, 1, 9, 9, 0);
        testNumberContainer(999.95e-3, opts, "999.9e-3", 0, 4, 4, 9999, 9999, -3);
        testNumberContainer(19.95e-9, opts, "19.9e-9", 0, 10, 10, 199, 199, -9);
        testNumberContainer(999.95e-9, opts, "999.9e-9", 0, 10, 10, 9999, 9999, -9);
    }

    @Test
    void engineering_digitCount_rounding_minLessThanMax() {
        FormatOptions opts = getEngOpts().setDecimalPlaces(0, 1);
        testNumberContainer(1.95e0, opts, "2e0", 2, 0, 0, 0, 0, 0);
        testNumberContainer(999.95e-3, opts, "1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(19.95e-9, opts, "20e-9", 0, 9, 8, 20, 2, -9);
        testNumberContainer(999.95e-9, opts, "1e-6", 0, 6, 6, 1, 1, -6);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(1.95e0, opts, "1.9e0", 1, 1, 1, 9, 9, 0);
        testNumberContainer(999.95e-3, opts, "999.9e-3", 0, 4, 4, 9999, 9999, -3);
        testNumberContainer(19.95e-9, opts, "19.9e-9", 0, 10, 10, 199, 199, -9);
        testNumberContainer(999.95e-9, opts, "999.9e-9", 0, 10, 10, 9999, 9999, -9);
    }

    @Test
    void engineering_digitCount_rounding_maxEqualTo0() {
        FormatOptions opts = getEngOpts().setDecimalPlaces(0, 0);
        testNumberContainer(1.5e0, opts, "2e0", 2, 0, 0, 0, 0, 0);
        testNumberContainer(999.5e-3, opts, "1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(19.5e-9, opts, "20e-9", 0, 9, 8, 20, 2, -9);
        testNumberContainer(999.5e-9, opts, "1e-6", 0, 6, 6, 1, 1, -6);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(1.5e0, opts, "1e0", 1, 0, 0, 0, 0, 0);
        testNumberContainer(999.5e-3, opts, "999e-3", 0, 3, 3, 999, 999, -3);
        testNumberContainer(19.5e-9, opts, "19e-9", 0, 9, 9, 19, 19, -9);
        testNumberContainer(999.5e-9, opts, "999e-9", 0, 9, 9, 999, 999, -9);
    }

    @Test
    void engineering_digitCount_minLessThanMax() {
        FormatOptions opts = getEngOpts().setDecimalPlaces(0, 1);
        testNumberContainer(990e-3, opts, "990e-3", 0, 3, 2, 990, 99, -3);
        testNumberContainer(10e-6, opts, "10e-6", 0, 6, 5, 10, 1, -6);
    }

    @Test
    void engineering_digitCount_minEqualToMax() {
        FormatOptions opts = getEngOpts().setDecimalPlaces(1, 1);
        testNumberContainer(0.9, opts, "900.0e-3", 0, 4, 1, 9000, 9, -3);
        testNumberContainer(1e-5, opts, "10.0e-6", 0, 7, 5, 100, 1, -6);
    }

    @Test
    void engineering_digitCount_minCausesOverflowIntoFraction() {
        FormatOptions opts = getEngOpts().setDecimalPlaces(5, 5);
        testNumberContainer(1200, opts, "1.20000e3", 1200, 2, 0, 0, 0, 3);
        testNumberContainer(1200.50, opts, "1.20050e3", 1200, 2, 1, 50, 5, 3);
    }

    // endregion engineering

    // region compact

    @Test
    void compact_zero() {
        CompactOptions opts = getCompactOps();
        testNumberContainer(0, opts.setDecimalPlaces(2, 2), "0.00K", 0, 0, 0, 0, 0, 3);
        testNumberContainer(0, opts.setDecimalPlaces(0, 2), "0K", 0, 0, 0, 0, 0, 3);
        testNumberContainer(0, opts.setDecimalPlaces(0, 0), "0K", 0, 0, 0, 0, 0, 3);
    }

    @Test
    void compact_general_positive() {
        CompactOptions opts = getCompactOps();
        // out threshold
        testNumberContainer(0.99, opts, "0K", 0, 0, 0, 0, 0, 3);
        testNumberContainer(500, opts, "0.5K", 500, 0, 0, 0, 0, 3);
        testNumberContainer(990, opts, "0.99K", 990, 0, 0, 0, 0, 3);

        // in threshold
        // Stream.of(
        // IntObjectImmutablePair.of(3, NumberFormatUtil.LOC_CACHE.getAbbreviationThousand()),
        // IntObjectImmutablePair.of(6, NumberFormatUtil.LOC_CACHE.getAbbreviationMillion()),
        // IntObjectImmutablePair.of(9, NumberFormatUtil.LOC_CACHE.getAbbreviationBillion()),
        // IntObjectImmutablePair.of(12, NumberFormatUtil.LOC_CACHE.getAbbreviationTrillion()),
        // IntObjectImmutablePair.of(15, NumberFormatUtil.LOC_CACHE.getAbbreviationQuadrillion()))
        // .forEach(pair -> {
        // int exp = pair.leftInt();
        // String abbvr = pair.right();
        // // not very memory efficient but eh, this is just testing code
        // Stream.of(
        // new BigDecimal("1.12"),
        // new BigDecimal("1.1"),
        // new BigDecimal("1"),
        // new BigDecimal("12.34"),
        // new BigDecimal("12.3"),
        // new BigDecimal("12"),
        // new BigDecimal("10"),
        // new BigDecimal("123.45"),
        // new BigDecimal("123.4"),
        // new BigDecimal("123"),
        // new BigDecimal("120"),
        // new BigDecimal("100"),
        // new BigDecimal("999.99")).forEach(bd -> {
        // BigInteger bi = bd.scaleByPowerOfTen(exp).toBigInteger();
        // testNumberContainer(bi, opts, bd.toPlainString() + abbvr, bi, 0, 0, 0, 0, exp);
        // });
        // });
    }

    @Test
    void compact_general_negative() {
        CompactOptions opts = getCompactOps();
        // out threshold
        testNumberContainer(-0.99, opts, "0K", 0, 0, 0, 0, 0, 3);
        testNumberContainer(-500, opts, "-0.5K", 500, 0, 0, 0, 0, 3);
        testNumberContainer(-990, opts, "-0.99K", 990, 0, 0, 0, 0, 3);

        // in threshold
        // Stream.of(
        // IntObjectImmutablePair.of(3, NumberFormatUtil.LOC_CACHE.getAbbreviationThousand()),
        // IntObjectImmutablePair.of(6, NumberFormatUtil.LOC_CACHE.getAbbreviationMillion()),
        // IntObjectImmutablePair.of(9, NumberFormatUtil.LOC_CACHE.getAbbreviationBillion()),
        // IntObjectImmutablePair.of(12, NumberFormatUtil.LOC_CACHE.getAbbreviationTrillion()),
        // IntObjectImmutablePair.of(15, NumberFormatUtil.LOC_CACHE.getAbbreviationQuadrillion()))
        // .forEach(pair -> {
        // int exp = pair.leftInt();
        // String abbvr = pair.right();
        // // not very memory efficient but eh, this is just testing code
        // Stream.of(
        // new BigDecimal("-1.12"),
        // new BigDecimal("-1.1"),
        // new BigDecimal("-1"),
        // new BigDecimal("-12.34"),
        // new BigDecimal("-12.3"),
        // new BigDecimal("-12"),
        // new BigDecimal("-10"),
        // new BigDecimal("-123.45"),
        // new BigDecimal("-123.4"),
        // new BigDecimal("-123"),
        // new BigDecimal("-120"),
        // new BigDecimal("-100"),
        // new BigDecimal("-999.99")).forEach(bd -> {
        // BigInteger bi = bd.stripTrailingZeros().scaleByPowerOfTen(exp).toBigInteger();
        // testNumberContainer(bi, opts, bd.toPlainString() + abbvr, bi.abs(), 0, 0, 0, 0, exp);
        // });
        // });
    }

    @Test
    void compact_digitCount_rounding_inflation_minEqualToMax() {
        CompactOptions opts = new CompactOptions().setDecimalPlaces(1, 1);
        testNumberContainer(999.95, opts, "1,000.0", 1000, 1, 0, 0, 0, 0);
        // compact should not inflate the unit even when set to a rounding that would cause it to go up.
        testNumberContainer(999.99e3, opts, "999.9K", 999.9e3, 0, 0, 0, 0, 3);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        // compact notation only triggers once the number is actually reached so it won't inflate if the starting value
        // is below the threshold, this may be something good to address in the future.
        testNumberContainer(999.95, opts, "999.9", 999, 1, 1, 9, 9, 0);
        // compact should not inflate the unit even when set to a rounding that would cause it to go up.
        testNumberContainer(999.95e3, opts, "999.9K", 999.9e3, 0, 0, 0, 0, 3);
    }

    @Test
    void compact_digitCount_rounding_inflation_minLessThanMax() {
        CompactOptions opts = getCompactOps().setDecimalPlaces(0, 1);
        testNumberContainer(999.95, opts, "1K", 1000, 0, 0, 0, 0, 3);
        // compact should not inflate the unit even when set to a rounding that would cause it to go up.
        testNumberContainer(999.99e3, opts, "999.9K", 999.9e3, 0, 0, 0, 0, 3);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(1950, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        // compact should not inflate the unit even when set to a rounding that would cause it to go up.
        testNumberContainer(999.95e3, opts, "999.9K", 999.9e3, 0, 0, 0, 0, 3);
    }

    @Test
    void compact_digitCount_rounding_inflation_maxEqualTo0() {
        CompactOptions opts = getCompactOps().setDecimalPlaces(0, 0);
        testNumberContainer(999.5e3, opts, "999K", 999e3, 0, 0, 0, 0, 3);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        // compact should not inflate the unit even when set to a rounding that would cause it to go up.
        testNumberContainer(999.5e3, opts, "999K", 999e3, 0, 0, 0, 0, 3);
    }

    @Test
    void compact_digitCount_rounding_minEqualToMax() {
        CompactOptions opts = getCompactOps().setDecimalPlaces(1, 1);
        testNumberContainer(950, opts, "1.0K", 1e3, 0, 0, 0, 0, 3);
        testNumberContainer(998.95e3, opts, "999.0K", 999e3, 0, 0, 0, 0, 3);
        testNumberContainer(1900, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        testNumberContainer(1950, opts, "2.0K", 2e3, 0, 0, 0, 0, 3);
        testNumberContainer(1900.5, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(950, opts, "0.9K", 900, 0, 0, 0, 0, 3);
        testNumberContainer(998.95e3, opts, "998.9K", 998.9e3, 0, 0, 0, 0, 3);
        testNumberContainer(1900, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        testNumberContainer(1950, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        testNumberContainer(1900.5, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
    }

    @Test
    void compact_digitCount_rounding_minLessThanMax() {
        CompactOptions opts = getCompactOps().setDecimalPlaces(0, 1);
        testNumberContainer(150, opts, "0.2K", 0.2e3, 0, 0, 0, 0, 3);
        testNumberContainer(950, opts, "1K", 1e3, 0, 0, 0, 0, 3);
        testNumberContainer(998.95e3, opts, "999K", 999e3, 0, 0, 0, 0, 3);
        testNumberContainer(1900, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        testNumberContainer(1950, opts, "2K", 2e3, 0, 0, 0, 0, 3);
        testNumberContainer(1900.5, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(150, opts, "0.1K", 0.1e3, 0, 0, 0, 0, 3);
        testNumberContainer(950, opts, "0.9K", 900, 0, 0, 0, 0, 3);
        testNumberContainer(998.95e3, opts, "998.9K", 998.9e3, 0, 0, 0, 0, 3);
        testNumberContainer(1900, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        testNumberContainer(1950, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
        testNumberContainer(1900.5, opts, "1.9K", 1.9e3, 0, 0, 0, 0, 3);
    }

    @Test
    void compact_digitCount_rounding_maxEqualTo0() {
        CompactOptions opts = getCompactOps().setDecimalPlaces(0, 0);
        testNumberContainer(500, opts, "1K", 1e3, 0, 0, 0, 0, 3);
        testNumberContainer(998.5e3, opts, "999K", 999e3, 0, 0, 0, 0, 3);
        testNumberContainer(1500, opts, "2K", 2e3, 0, 0, 0, 0, 3);
        testNumberContainer(1000.5, opts, "1K", 1e3, 0, 0, 0, 0, 3);
        opts.setRoundingMode(RoundingMode.HALF_DOWN);
        testNumberContainer(500, opts, "0K", 0, 0, 0, 0, 0, 3);
        testNumberContainer(998.5e3, opts, "998K", 998e3, 0, 0, 0, 0, 3);
        testNumberContainer(1500, opts, "1K", 1e3, 0, 0, 0, 0, 3);
        testNumberContainer(1000.5, opts, "1K", 1e3, 0, 0, 0, 0, 3);
    }

    @Test
    void compact_digitCount_minLessThanMax() {
        CompactOptions opts = getCompactOps().setDecimalPlaces(0, 1);
        testNumberContainer(1000, opts, "1K", 1000, 0, 0, 0, 0, 3);
        testNumberContainer(1200, opts, "1.2K", 1200, 0, 0, 0, 0, 3);
        testNumberContainer(1e6, opts, "1M", 1e6, 0, 0, 0, 0, 6);
        testNumberContainer(1.2e6, opts, "1.2M", 1.2e6, 0, 0, 0, 0, 6);
        opts.setDecimalPlaces(0, 3);
        testNumberContainer(1234, opts, "1.234K", 1234, 0, 0, 0, 0, 3);
    }

    @Test
    void compact_digitCount_minEqualToMax() {
        CompactOptions opts = getCompactOps().setDecimalPlaces(1, 1);
        testNumberContainer(1000, opts, "1.0K", 1000, 0, 0, 0, 0, 3);
        testNumberContainer(1200, opts, "1.2K", 1200, 0, 0, 0, 0, 3);
        testNumberContainer(1e6, opts, "1.0M", 1e6, 0, 0, 0, 0, 6);
        testNumberContainer(1.2e6, opts, "1.2M", 1.2e6, 0, 0, 0, 0, 6);
        opts.setDecimalPlaces(3, 3);
        testNumberContainer(1234, opts, "1.234K", 1234, 0, 0, 0, 0, 3);
        testNumberContainer(1200, opts, "1.200K", 1200, 0, 0, 0, 0, 3);
        testNumberContainer(1e6, opts, "1.000M", 1e6, 0, 0, 0, 0, 6);
        testNumberContainer(1234e3, opts, "1.234M", 1234e3, 0, 0, 0, 0, 6);
    }

    @Test
    void compact_digitCount_minCanCreateFractionValues() {
        CompactOptions opts = getCompactOps().setDecimalPlaces(5, 5);
        testNumberContainer(1200, opts, "1.20000K", 1200, 2, 0, 0, 0, 3);
        testNumberContainer(1200.50, opts, "1.20050K", 1200, 2, 1, 50, 5, 3);
    }

    // endregion compact

    @Test
    void general() {

        FormatOptions opts = getBaseOpts();
        testNumberContainer(1, opts, "1", 1, 0, 0, 0, 0, 0);
        testNumberContainer(1.0, opts, "1", 1, 0, 0, 0, 0, 0);
        testNumberContainer(1.00, opts, "1", 1, 0, 0, 0, 0, 0);
        testNumberContainer(1.3, opts, "1.3", 1, 1, 1, 3, 3, 0);
        testNumberContainer(1.30, opts, "1.3", 1, 1, 1, 3, 3, 0);
        testNumberContainer(1.03, opts, "1.03", 1, 2, 2, 3, 3, 0);
        testNumberContainer(1.230, opts, "1.23", 1, 2, 2, 23, 23, 0);
        testNumberContainer(120_000, opts, "120,000", 120_000, 0, 0, 0, 0, 0);
        testNumberContainer(1200.50, opts, "1,200.5", 1200, 1, 1, 5, 5, 0);
        testNumberContainer(1.20050e3, opts, "1,200.5", 1200, 1, 1, 5, 5, 0);

        testNumberContainer(10.0, opts, "10", 10, 0, 0, 0, 0, 0);

        // check zero values
        testNumberContainer(0, getBaseOpts().setDecimalPlaces(2, 2), "0.00", 0, 2, 0, 0, 0, 0);
        testNumberContainer(0, getBaseOpts().setDecimalPlaces(0, 2), "0", 0, 0, 0, 0, 0, 0);
        testNumberContainer(0, getBaseOpts().setDecimalPlaces(0, 0), "0", 0, 0, 0, 0, 0, 0);

        // min decimal places works
        opts = getBaseOpts().setDecimalPlaces(2, 2);
        testNumberContainer("1.0", opts, "1.00", 1, 2, 0, 0, 0, 0);
        testNumberContainer(1, opts, "1.00", 1, 2, 0, 0, 0, 0);

        // min decimal places works
        opts = getBaseOpts().setDecimalPlaces(1, 2);
        testNumberContainer("1", opts, "1.0", 1, 1, 0, 0, 0, 0);
    }

    @ParameterizedTest
    @MethodSource("tryAllExponentialOptions")
    void exponential_threshold(FormatOptions opts) {
        opts.setExponentialThreshold(BD_MILLION);
        testNumberContainer(999_999.99, opts, "999,999.99", 999_999, 2, 2, 99, 99, 0);
        // rounding allows it to cross the threshold without triggering format
        testNumberContainer(999_999.995, opts, "1,000,000", 1e6, 0, 0, 0, 0, 0);
        testNumberContainer(1_000_000, opts, "1e6", 1e6, 0, 0, 0, 0, 6);
    }

    @ParameterizedTest
    @MethodSource("tryAllOptions")
    void specialValuesAlwaysReturnOther(NumberOptionsBase<?> opts) {
        // spotless:off
        testNumberContainer((Number) Double.POSITIVE_INFINITY, opts, NumberFormatUtil.LOC_CACHE.getSpecialValueInfinity(), BigDecimal.ZERO, BigInteger.ZERO, -1, 0, BigInteger.ZERO, BigInteger.ZERO, 0);
        testNumberContainer((Number) Double.NEGATIVE_INFINITY, opts, NumberFormatUtil.LOC_CACHE.getSpecialValueNegInfinity(), BigDecimal.ZERO, BigInteger.ZERO, -1, 0, BigInteger.ZERO, BigInteger.ZERO, 0);
        testNumberContainer((Number) Double.NaN, opts, NumberFormatUtil.LOC_CACHE.getSpecialValueNaN(), BigDecimal.ZERO, BigInteger.ZERO, -1, 0, BigInteger.ZERO, BigInteger.ZERO, 0);
        testNumberContainer(null, opts, NumberFormatUtil.LOC_CACHE.getSpecialValueNull(), BigDecimal.ZERO, BigInteger.ZERO, -1, 0, BigInteger.ZERO, BigInteger.ZERO, 0);
        // spotless:on
    }

    private void testNumberContainer(String number, NumberOptionsBase<?> options, String expectedOutput, Number i,
            Number v, Number w, Number f, Number t, Number c) {
        testNumberContainer(parseBigDecimal(number, options), options, expectedOutput, i, v, w, f, t, c);
    }

    private void testNumberContainer(Number number, NumberOptionsBase<?> options, String expectedOutput, Number i,
            Number v, Number w, Number f, Number t, Number c) {
        String n = expectedOutput.startsWith("-") ? expectedOutput.substring(1) : expectedOutput;
        testNumberContainer(number, options, expectedOutput, n, i, v, w, f, t, c);
    }

    private void testNumberContainer(Number number, NumberOptionsBase<?> options, String expectedOutput, Number n,
            Number i, Number v, Number w, Number f, Number t, Number c) {
        testNumberContainer(
                number,
                options,
                expectedOutput,
                NumberFormatUtil.bigDecimalConverter(n),
                parseBigInteger(i),
                v.intValue(),
                w.intValue(),
                parseBigInteger(f),
                parseBigInteger(t),
                c.intValue());
    }

    private void testNumberContainer(Number number, NumberOptionsBase<?> options, String expectedOutput, String n,
            Number i, Number v, Number w, Number f, Number t, Number c) {
        testNumberContainer(
                number,
                options,
                expectedOutput,
                parseBigDecimal(n, options),
                parseBigInteger(i),
                v.intValue(),
                w.intValue(),
                parseBigInteger(f),
                parseBigInteger(t),
                c.intValue());
    }

    private void testNumberContainer(Number number, NumberOptionsBase<?> options, String expectedOutput, BigDecimal n,
            BigInteger i, int v, int w, BigInteger f, BigInteger t, int c) {

        if (options.getExponentialFormat() == ExponentialFormat.POWER_OF_TEN) {
            expectedOutput = expectedOutput.replace("e", "*10^");
        }

        CLDRPluralRuleNumber container = new CLDRPluralRuleNumber(number, options);
        String actualOutput = options.format(number);
        // create a better error readout
        String finalExpectedOutput = expectedOutput;
        BigDecimal finalExpectedN = n.stripTrailingZeros();
        BigDecimal finalActualN = container.n.stripTrailingZeros();

        String errText = createErrorDisplay(
                new ErrLine("output", finalExpectedOutput, actualOutput),
                new ErrLine("n", finalExpectedN, finalActualN),
                new ErrLine("i", i, container.i),
                new ErrLine("v", v, container.v),
                new ErrLine("w", w, container.w),
                new ErrLine("f", f, container.f),
                new ErrLine("t", t, container.t),
                new ErrLine("c", c, container.c));

        assertAll(errText, () -> {
            assertEquals(finalExpectedOutput, actualOutput, "Unexpected output for " + number);
            assertEquals(finalExpectedN, finalActualN, "n value is incorrect for " + number);
            assertEquals(i, container.i, "i value is incorrect for " + number);
            assertEquals(v, container.v, "v value is incorrect for " + number);
            assertEquals(w, container.w, "w value is incorrect for " + number);
            assertEquals(f, container.f, "f value is incorrect for " + number);
            assertEquals(t, container.t, "t value is incorrect for " + number);
            assertEquals(c, container.c, "c value is incorrect for " + number);
        });

    }

    private static BigInteger parseBigInteger(Number number) {
        if (number instanceof BigInteger bi) {
            return bi;
        } else if (number instanceof BigDecimal bd) {
            return bd.toBigInteger();
        }
        return BigInteger.valueOf(number.longValue());
    }

    private static BigDecimal parseBigDecimal(String unparsed, NumberOptionsBase<?> options) {
        unparsed = unparsed.replace("_", ",");
        // fix up exponential notation
        if (options.isExponentialFormattingEnabled()) {
            unparsed = unparsed.replace("e", "E");
            unparsed = unparsed.replace("*10^", "E");
            unparsed = unparsed.replace("x10^", "E");
        }
        DecimalFormat df = new DecimalFormat();
        df.setParseBigDecimal(true);
        int scaleBy = 0;
        // fix up compact notation
        if (options instanceof CompactOptions) {
            for (var threshold : NumberFormatUtil.LOC_CACHE.getCompactDividerThresholds()) {
                if (unparsed.endsWith(threshold.abbreviation)) {
                    unparsed = unparsed.substring(0, unparsed.length() - threshold.abbreviation.length());
                    scaleBy = threshold.number.precision() - 1 - threshold.number.scale();
                    break;
                }
            }
        }
        Number ret;
        try {
            ret = df.parse(unparsed);
        } catch (ParseException e) {
            fail("could not parse " + unparsed + " to big decimal");
            throw new RuntimeException(e);
        }
        return NumberFormatUtil.bigDecimalConverter(ret).stripTrailingZeros().scaleByPowerOfTen(scaleBy);
    }

    private static final class ErrLine {

        public final String name;
        public final String expected;
        public final String actual;
        public final boolean pass;

        private ErrLine(String name, Object expected, Object actual) {
            this.name = name;
            this.expected = expected.toString();
            this.actual = actual.toString();
            this.pass = actual.equals(expected);
        }

    }

    private String createErrorDisplay(ErrLine... lines) {
        final String value = "value";
        final String expected = "expected";
        final String actual = "actual";
        final String pass = "pass";
        int[] lengths = new int[] { value.length(), expected.length(), actual.length(), pass.length() };
        StringBuilder sb = new StringBuilder();
        // some shenanigans to get the thing to print out more clearly
        sb.append("\u00A0").append(System.lineSeparator());
        for (ErrLine line : lines) {
            lengths[0] = Math.max(lengths[0], line.name.length());
            lengths[1] = Math.max(lengths[1], line.expected.length());
            lengths[2] = Math.max(lengths[2], line.actual.length());
        }
        String formatString = "\033[%sm%-"
                + String.join("s | %-", Arrays.stream(lengths).mapToObj(Integer::toString).toArray(String[]::new))
                + "s\033[0m";
        sb.append(String.format(formatString, "31", value, expected, actual, pass));
        sb.append(System.lineSeparator());
        sb.append(
                String.format(
                        formatString,
                        "31",
                        StringUtils.repeat('-', lengths[0]),
                        StringUtils.repeat('-', lengths[1]),
                        StringUtils.repeat('-', lengths[2]),
                        StringUtils.repeat('-', lengths[3])).replace(' ', '-'));
        for (ErrLine line : lines) {
            sb.append(System.lineSeparator());
            sb.append(
                    String.format(
                            formatString,
                            line.pass ? "32" : "31",
                            line.name,
                            line.expected,
                            line.actual,
                            line.pass ? "OK" : "FAIL"));;
        }
        // some shenanigans to get the thing to print out more clearly
        sb.append(System.lineSeparator()).append("\u00A0");
        return sb.toString();
    }
}
