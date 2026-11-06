package com.gtnewhorizon.gtnhlib.test.util;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_BILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_MILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_QUADRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_THOUSAND;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_TRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.postConfiguration;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.extensions.DefaultExponentialFormat;
import com.gtnewhorizon.gtnhlib.extensions.DisableFormattedNotation;
import com.gtnewhorizon.gtnhlib.extensions.ForceDisplayLocale;
import com.gtnewhorizon.gtnhlib.extensions.ForceFormatLocale;
import com.gtnewhorizon.gtnhlib.extensions.GlobalDisableExponentialNotation;
import com.gtnewhorizon.gtnhlib.util.numberformatting.ExponentialFormat;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;

@ForceFormatLocale("en-US")
@ForceDisplayLocale("en-US")
@DisableFormattedNotation(false)
@GlobalDisableExponentialNotation(false)
@DefaultExponentialFormat(ExponentialFormat.SCIENTIFIC)
public class NumberFormatUtilTest {

    /* ========================= Rollover =============================== */

    @Test
    void abbreviationRolloverRules() {
        assertEquals("999.99K", NumberFormatUtil.formatNumberCompact(999_999));
        assertEquals("999.99M", NumberFormatUtil.formatNumberCompact(999_999_999));

        assertEquals("1M", NumberFormatUtil.formatNumberCompact(999_999 + 2));
        assertEquals("1B", NumberFormatUtil.formatNumberCompact(999_999_999 + 2));
    }

    /* ========================= Locale / Plain ========================= */

    @Test
    void usLocalePlainFormatting() {
        assertEquals("12.3", NumberFormatUtil.formatNumber(12.3));
        assertEquals("1,234.56", NumberFormatUtil.formatNumber(1234.56));
        assertEquals("1,234.13", NumberFormatUtil.formatNumber(1234.125));
        assertEquals("-1,234.56", NumberFormatUtil.formatNumber(-1234.56));
        assertEquals("1,000,001", NumberFormatUtil.formatNumber(1_000_000.999));

        assertEquals("NaN", NumberFormatUtil.formatNumber(Double.NaN));
        assertEquals("Infinity", NumberFormatUtil.formatNumber(Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", NumberFormatUtil.formatNumber(Double.NEGATIVE_INFINITY));
    }

    @Test
    @ForceFormatLocale("fr-FR")
    void frenchLocalePlainFormatting() {
        assertEquals("12,3", NumberFormatUtil.formatNumber(12.3));
        assertEquals("1 234,56", NumberFormatUtil.formatNumber(1234.56));
        assertEquals("1 234,13", NumberFormatUtil.formatNumber(1234.125));
        assertEquals("-1 234,56", NumberFormatUtil.formatNumber(-1234.56));
        assertEquals("1 000 001", NumberFormatUtil.formatNumber(1_000_000.999));
    }

    /* ========================= Compact / Abbreviation ========================= */

    @Test
    @ForceFormatLocale("fr-FR")
    void abbreviationRespectsLocaleDecimalSeparator() {
        assertEquals("1,23M", NumberFormatUtil.formatNumberCompact(1_234_567));
    }

    @Test
    void extremeFormatCompact() {
        assertEquals("NaN", NumberFormatUtil.formatNumberCompact(Double.NaN));
        assertEquals("Infinity", NumberFormatUtil.formatNumberCompact(Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", NumberFormatUtil.formatNumberCompact(Double.NEGATIVE_INFINITY));
    }

    @Test
    void bigIntegerCompactFormattingMatchesLong() {
        assertEquals(
                NumberFormatUtil.formatNumberCompact(1_234_567L),
                NumberFormatUtil.formatNumberCompact(new BigInteger("1234567")));
    }

    /* ========================= Significant Digits ========================= */

    @Test
    void sigFigCompactFormattingCases() {
        assertEquals("180K", NumberFormatUtil.formatNumberCompact(180_000));
        assertEquals("180.2K", NumberFormatUtil.formatNumberCompact(180_200));
        assertEquals("180.9K", NumberFormatUtil.formatNumberCompact(180_900));
        assertEquals("180.23K", NumberFormatUtil.formatNumberCompact(180_230));
        assertEquals("180.23K", NumberFormatUtil.formatNumberCompact(180_233));
        assertEquals("180.24K", NumberFormatUtil.formatNumberCompact(180_239));
    }

    @Test
    void decimalPlacesCompactFormattingCases() {
        assertEquals(
                "9.999481312833312K",
                NumberFormatUtil.formatNumberCompact(9999.481312833312, new CompactOptions().setDecimalPlaces(1_000)));
        assertEquals(
                "9.1111111119K",
                NumberFormatUtil.formatNumberCompact(9111.1111119, new CompactOptions().setDecimalPlaces(10)));
        assertEquals(
                "9.111111112K",
                NumberFormatUtil.formatNumberCompact(9111.1111119, new CompactOptions().setDecimalPlaces(9)));
    }

    /* ========================= Zero / Edge ========================= */

    @Test
    void zeroIsAlwaysRenderedAsZero() {
        postConfiguration();

        assertEquals("0", NumberFormatUtil.formatNumber(0));
        assertEquals("0", NumberFormatUtil.formatNumber(-0.0));
        assertEquals("0", NumberFormatUtil.formatNumber(BigInteger.ZERO));

        assertEquals("0", NumberFormatUtil.formatNumberCompact(0));
        assertEquals("0", NumberFormatUtil.formatNumberCompact(-0.0));
        assertEquals("0", NumberFormatUtil.formatNumberCompact(BigInteger.ZERO));
    }

    /* ========================= Disable Formatting ========================= */

    @Test
    @DisableFormattedNotation(true)
    void disableFormattedNotationBypassesAllFormatting() {
        assertEquals("1000000", NumberFormatUtil.formatNumber(1_000_000));
        assertEquals("1000000", NumberFormatUtil.formatFluidCompact(1_000_000).split(" ")[0]);
        assertEquals(String.valueOf(1_000_000_000_000L), NumberFormatUtil.formatNumber(1_000_000_000_000L));
    }

    @Test
    void decimalPlacesOptions() {
        double val = 123.456789;

        FormatOptions formatOptions = new FormatOptions().setDecimalPlaces(1);
        CompactOptions compactOptions = new CompactOptions().setDecimalPlaces(1);

        formatOptions.setDecimalPlaces(0);
        compactOptions.setDecimalPlaces(0);

        assertEquals("101", NumberFormatUtil.formatNumber(100.99, formatOptions));
        assertEquals("101", NumberFormatUtil.formatNumberCompact(100.99, compactOptions));

        assertEquals("100", NumberFormatUtil.formatNumber(100.4999, formatOptions));
        assertEquals("100", NumberFormatUtil.formatNumberCompact(100.4999, compactOptions));

        // Non-zero decimal place rules.

        formatOptions.setDecimalPlaces(1);
        compactOptions.setDecimalPlaces(1);

        assertEquals("123.5", NumberFormatUtil.formatNumber(val, formatOptions));
        assertEquals("123.5", NumberFormatUtil.formatNumberCompact(val, compactOptions));

        formatOptions.setDecimalPlaces(2);
        compactOptions.setDecimalPlaces(2);

        assertEquals("123.46", NumberFormatUtil.formatNumber(val, formatOptions));
        assertEquals("123.46", NumberFormatUtil.formatNumberCompact(val, compactOptions));

        formatOptions.setDecimalPlaces(100);
        compactOptions.setDecimalPlaces(100);

        assertEquals("123.456789", NumberFormatUtil.formatNumber(val, formatOptions));
        assertEquals("123.456789", NumberFormatUtil.formatNumberCompact(val, compactOptions));

        // Decimal places is irrelevant.
        assertEquals("1e12", NumberFormatUtil.formatNumber(1_000_000_000_000L, formatOptions));
        assertEquals("1e12", NumberFormatUtil.formatNumberCompact(1_000_000_000_000L, compactOptions));
    }

    @Test
    void compactThresholdOptions() {
        CompactOptions opts = new CompactOptions().setCompactThreshold(10_000);;

        // Just below threshold
        assertEquals("9,999", NumberFormatUtil.formatNumberCompact(9_999, opts));

        // At threshold
        assertEquals("10K", NumberFormatUtil.formatNumberCompact(10_000, opts));

        // Just above threshold
        assertEquals("10K", NumberFormatUtil.formatNumberCompact(10_001, opts));

        // Just above threshold with decimal options set properly.
        opts.setDecimalPlaces(3);
        assertEquals("10.001K", NumberFormatUtil.formatNumberCompact(10_001, opts));
    }

    @Test
    void allNumberTypesFormatting() {
        Number[] numbers = { (byte) 123, (short) 1234, 12345, // Integer
                123456789L, // Long
                1234.567F, // Float
                12345.6789, // Double
                new BigInteger("1234567890123456789"), new BigDecimal("12345.67890123456789") };

        for (Number n : numbers) {
            String plain = NumberFormatUtil.formatNumber(n);
            String compact = NumberFormatUtil.formatNumberCompact(n);

            // sanity checks: not null, not empty
            assertNotNull(plain, "formatNumber returned null for " + n.getClass());
            assertFalse(plain.isEmpty(), "formatNumber returned empty string for " + n.getClass());

            assertNotNull(compact, "formatNumberCompact returned null for " + n.getClass());
            assertFalse(compact.isEmpty(), "formatNumberCompact returned empty string for " + n.getClass());
        }
    }

    @Test
    void testCompactBounds() {
        CompactOptions options = new CompactOptions();
        options.disableExponentialFormatting();

        // K, M, B, T, Q thresholds (scientific off)
        assertEquals("1K", NumberFormatUtil.formatNumberCompact(BD_THOUSAND, options));
        assertEquals("1M", NumberFormatUtil.formatNumberCompact(BD_MILLION, options));
        assertEquals("1B", NumberFormatUtil.formatNumberCompact(BD_BILLION, options));
        assertEquals("1T", NumberFormatUtil.formatNumberCompact(BD_TRILLION, options));
        assertEquals("1Q", NumberFormatUtil.formatNumberCompact(BD_QUADRILLION, options));
        assertEquals("1,000Q", NumberFormatUtil.formatNumberCompact(BD_TRILLION.multiply(BD_MILLION), options));
        assertEquals(
                "1,000,000,000Q",
                NumberFormatUtil.formatNumberCompact(BD_TRILLION.multiply(BD_TRILLION), options));

        assertEquals("999.99K", NumberFormatUtil.formatNumberCompact(999_999L, options));
    }

    @Test
    void testCompactRoundingModes() {
        CompactOptions options = new CompactOptions();

        // FLOOR: never inflate
        options.setRoundingMode(RoundingMode.FLOOR);
        assertEquals("9.99K", NumberFormatUtil.formatNumberCompact(9_999, options));
        assertEquals("999.99B", NumberFormatUtil.formatNumberCompact(999_999_999_999L, options));

        // CEILING: always inflate
        options.setRoundingMode(RoundingMode.CEILING);
        assertEquals("9.01K", NumberFormatUtil.formatNumberCompact(9_001, options));
        assertEquals("999.99B", NumberFormatUtil.formatNumberCompact(999_990_000_001L, options));
    }

    @Test
    @DisableFormattedNotation(true)
    void disableFormattedNotation_isPlainAndNonScientific_forAllSupportedTypes() {

        // BigDecimal: exponent input must not round-trip to scientific notation
        BigDecimal bdExpPos = new BigDecimal("1000000E3"); // commonly prints "1.000000E+9" via toString()
        assertEquals("1000000000", NumberFormatUtil.formatNumber(bdExpPos));
        assertEquals("1000000000", NumberFormatUtil.formatNumberCompact(bdExpPos));

        // BigDecimal: large positive exponent (stress plain conversion size)
        BigDecimal bdHugePos = new BigDecimal("1E200");
        String bdHugePosOut = NumberFormatUtil.formatNumber(bdHugePos);
        assertEquals(bdHugePos.toPlainString(), bdHugePosOut);
        assertFalse(bdHugePosOut.contains("e"));
        assertFalse(bdHugePosOut.contains(" "));
        assertFalse(bdHugePosOut.contains(","));

        // BigDecimal: large negative exponent (stress leading "0.00...1")
        BigDecimal bdHugeNeg = new BigDecimal("1E-200");
        String bdHugeNegOut = NumberFormatUtil.formatNumber(bdHugeNeg);
        assertEquals(bdHugeNeg.toPlainString(), bdHugeNegOut);
        assertFalse(bdHugeNegOut.contains("e"));
        assertFalse(bdHugeNegOut.contains(" "));
        assertFalse(bdHugeNegOut.contains(","));

        // BigInteger: should remain plain digits
        BigInteger bi = new BigInteger("123456789012345678901234567890");
        assertEquals(bi.toString(), NumberFormatUtil.formatNumber(bi));
        assertEquals(bi.toString(), NumberFormatUtil.formatNumberCompact(bi));

        // Atomic types: must be digits, no grouping
        AtomicLong al = new AtomicLong(9_007_199_254_740_993L); // 2^53 + 1
        assertEquals("9007199254740993", NumberFormatUtil.formatNumber(al));
        assertEquals("9007199254740993", NumberFormatUtil.formatNumberCompact(al));

        AtomicInteger ai = new AtomicInteger(1234567);
        assertEquals("1234567", NumberFormatUtil.formatNumber(ai));
        assertEquals("1234567", NumberFormatUtil.formatNumberCompact(ai));

        // Floating-point
        assertEquals("0", NumberFormatUtil.formatNumber(-0.0d));
        assertEquals("NaN", NumberFormatUtil.formatNumber(Double.NaN));
        assertEquals("Infinity", NumberFormatUtil.formatNumber(Double.POSITIVE_INFINITY));
        assertEquals("-Infinity", NumberFormatUtil.formatNumber(Double.NEGATIVE_INFINITY));

        assertEquals("1234.5", NumberFormatUtil.formatNumber(1234.5d));
        assertEquals("0.000001", NumberFormatUtil.formatNumber(0.000001d));
        assertEquals("1000000000000", NumberFormatUtil.formatNumber(1e12d));
        assertEquals("1000000", NumberFormatUtil.formatNumber(1e6f));

        // Units must still append, but number part must remain raw (no suffix, no grouping)
        assertEquals("1000000 " + NumberFormatUtil.getFluidUnit(), NumberFormatUtil.formatFluidCompact(1_000_000));
        assertEquals("1000000 " + NumberFormatUtil.getEnergyUnit(), NumberFormatUtil.formatEnergyCompact(1_000_000));

        // Stress test.
        for (int exp = -300; exp <= 300; exp++) {
            BigDecimal sweep = new BigDecimal("1E" + exp);
            String out = NumberFormatUtil.formatNumber(sweep);
            assertEquals(sweep.toPlainString(), out, "Mismatch for exponent " + exp);
            assertFalse(out.contains("e"), "Scientific notation leaked for exponent " + exp);
            assertFalse(out.contains(" "), "Grouping leaked for exponent " + exp);
            assertFalse(out.contains(","), "Grouping leaked for exponent " + exp);
        }
    }

    @Test
    void setScientificThreshold_largeLongs_shouldRoundTripExactly() {
        // 2^53 is the last integer that a double can represent exactly.
        // Anything above that will start skipping odd values.
        long start = 9_007_199_254_740_993L; // 2^53 + 1

        FormatOptions opts = new FormatOptions();

        // Many values here cannot be represented exactly as double.
        for (long v = start; v < start + 10_000L; v++) {
            opts.setExponentialThreshold(v);

            BigDecimal expected = BigDecimal.valueOf(v);
            BigDecimal actual = opts.getExponentialThreshold();

            assertEquals(expected, actual, "Lost precision converting long threshold: " + v);
        }
    }
}
