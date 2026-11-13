package com.gtnewhorizon.gtnhlib.test.util;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_THOUSAND;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_TRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.RoundingMode;
import java.util.Locale;

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
public class ExponentialFormatTest {

    private static final double VALUE = 1.23456789e12; // 1.23 trillion

    @Test
    @DefaultExponentialFormat(ExponentialFormat.SCIENTIFIC)
    void setScientificLocale() {
        double value = 1_888_888_888_888L;

        Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
        assertEquals("1,89e12", NumberFormatUtil.formatNumber(value));
        assertEquals("1,89e12", NumberFormatUtil.formatNumberCompact(value));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.SCIENTIFIC)
    void scientificFormat_formatsUsingExponent() {
        assertEquals("1.23e12", NumberFormatUtil.formatNumber(VALUE));
        assertEquals("1.23e13", NumberFormatUtil.formatNumber(VALUE * 10));
        assertEquals("1.23e14", NumberFormatUtil.formatNumber(VALUE * 100));

        assertEquals("-1.23e12", NumberFormatUtil.formatNumber(-VALUE));
        assertEquals("-1.23e13", NumberFormatUtil.formatNumber(-VALUE * 10));
        assertEquals("-1.23e14", NumberFormatUtil.formatNumber(-VALUE * 100));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.ENGINEERING)
    void engineeringFormat_formatsUsingExponent() {
        assertEquals("1.23e12", NumberFormatUtil.formatNumber(VALUE));
        assertEquals("12.35e12", NumberFormatUtil.formatNumber(VALUE * 10));
        assertEquals("123.46e12", NumberFormatUtil.formatNumber(VALUE * 100));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.POWER_OF_TEN)
    void powerOfTenFormat_formatsUsingExplicitPower() {
        assertEquals("1.23*10^12", NumberFormatUtil.formatNumber(VALUE));
        assertEquals("1.23*10^13", NumberFormatUtil.formatNumber(VALUE * 10));
        assertEquals("1.23*10^14", NumberFormatUtil.formatNumber(VALUE * 100));

        assertEquals("-1.23*10^12", NumberFormatUtil.formatNumber(-VALUE));
        assertEquals("-1.23*10^13", NumberFormatUtil.formatNumber(-VALUE * 10));
        assertEquals("-1.23*10^14", NumberFormatUtil.formatNumber(-VALUE * 100));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.SCIENTIFIC)
    void scientificFormat_roundingBehavior() {
        assertEquals("1.23e12", NumberFormatUtil.formatNumber(1.234e12));
        assertEquals("1.24e12", NumberFormatUtil.formatNumber(1.235e12));
        assertEquals("1.24e12", NumberFormatUtil.formatNumber(1.239e12));

        assertEquals("-1.23e12", NumberFormatUtil.formatNumber(-1.234e12));
        assertEquals("-1.24e12", NumberFormatUtil.formatNumber(-1.235e12));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.ENGINEERING)
    void engineeringFormat_roundingBehavior() {
        // Mantissa in [1, 10)
        assertEquals("1.23e12", NumberFormatUtil.formatNumber(1.234e12));
        assertEquals("1.24e12", NumberFormatUtil.formatNumber(1.235e12));

        // Mantissa in [10, 100)
        assertEquals("12.34e12", NumberFormatUtil.formatNumber(1.234e13));
        assertEquals("12.35e12", NumberFormatUtil.formatNumber(1.235e13));

        // Mantissa in [100, 1000)
        assertEquals("123.45e12", NumberFormatUtil.formatNumber(1.2345e14));
        assertEquals("123.56e12", NumberFormatUtil.formatNumber(1.23555e14));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.POWER_OF_TEN)
    void powerOfTenFormat_roundingBehavior() {
        assertEquals("1.23*10^12", NumberFormatUtil.formatNumber(1.234e12));
        assertEquals("1.24*10^12", NumberFormatUtil.formatNumber(1.235e12));

        assertEquals("1.23*10^13", NumberFormatUtil.formatNumber(1.234e13));
        assertEquals("1.24*10^13", NumberFormatUtil.formatNumber(1.235e13));

        assertEquals("-1.23*10^12", NumberFormatUtil.formatNumber(-1.234e12));
        assertEquals("-1.24*10^12", NumberFormatUtil.formatNumber(-1.235e12));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.ENGINEERING)
    void engineeringFormat_rolloverOnRounding() {
        assertEquals("1e15", NumberFormatUtil.formatNumber(9.99995e14));
        assertEquals("10e15", NumberFormatUtil.formatNumber(9.99995e15));
        assertEquals("100e15", NumberFormatUtil.formatNumber(9.99995e16));
        assertEquals("1e18", NumberFormatUtil.formatNumber(9.99995e17));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.ENGINEERING)
    void engineeringFormat_locale() {
        Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
        assertEquals("11,11e24", NumberFormatUtil.formatNumber(1.1111111e25));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.SCIENTIFIC)
    void scientificFormat_cutOff() {
        long threshold = BD_TRILLION.longValue();

        assertEquals("999,999,999,999", NumberFormatUtil.formatNumber(threshold - 1));
        assertEquals("1e12", NumberFormatUtil.formatNumber(threshold));
        assertEquals("1e12", NumberFormatUtil.formatNumber(threshold + 1));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.SCIENTIFIC)
    void scientificThresholdOption() {
        // Safe thresholds
        FormatOptions formatOptions = new FormatOptions().setExponentialThreshold(BD_TRILLION); // 1T
        CompactOptions compactOptions = new CompactOptions().setCompactThreshold(BD_THOUSAND) // 1K
                .setExponentialThreshold(BD_TRILLION); // 1T

        // Verify formatting respects thresholds
        assertEquals("1e12", NumberFormatUtil.formatNumber(BD_TRILLION, formatOptions));
        assertEquals("1K", NumberFormatUtil.formatNumberCompact(BD_THOUSAND, compactOptions));
        assertEquals("1e12", NumberFormatUtil.formatNumberCompact(BD_TRILLION, compactOptions));

        // Defaults are safe: compact 1K < scientific 1T
        CompactOptions defaultOptions = new CompactOptions();
        assertTrue(defaultOptions.getCompactThreshold().compareTo(defaultOptions.getExponentialThreshold()) < 0);

        // Unsafe: compact >= scientific should throw
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompactOptions().setExponentialThreshold(500).setCompactThreshold(500));

        assertThrows(
                IllegalArgumentException.class,
                () -> new CompactOptions().setCompactThreshold(2 * BD_TRILLION.longValue()));
    }

    @Test
    @DefaultExponentialFormat(ExponentialFormat.SCIENTIFIC)
    void testScientificRoundingAcrossMagnitude() {
        CompactOptions options = new CompactOptions();

        // CEILING crosses magnitude
        options.setRoundingMode(RoundingMode.CEILING);
        assertEquals("1.01e12", NumberFormatUtil.formatNumberCompact(1_000_000_000_001L, options));

        // HALF_UP rounds based on midpoint
        options.setRoundingMode(RoundingMode.HALF_UP);
        assertEquals("2e12", NumberFormatUtil.formatNumberCompact(1_995_000_000_001L, options));
        assertEquals("1.99e12", NumberFormatUtil.formatNumberCompact(1_990_000_000_001L, options));
    }

}
