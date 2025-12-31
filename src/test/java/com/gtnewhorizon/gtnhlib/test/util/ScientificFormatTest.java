package com.gtnewhorizon.gtnhlib.test.util;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_THOUSAND;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_TRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.RoundingMode;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;

public class ScientificFormatTest {

    private static final double VALUE = 1.23456789e12; // 1.23 trillion

    private String oldFormatPattern;
    private Locale oldLocale;

    @BeforeEach
    void setup() {
        oldFormatPattern = formatPattern;
        oldLocale = Locale.getDefault(Locale.Category.FORMAT);

        // Ensure deterministic output
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
    }

    @AfterEach
    void teardown() {
        NumberFormatConfig.formatPattern = oldFormatPattern;
        Locale.setDefault(Locale.Category.FORMAT, oldLocale);
        NumberFormatUtil.postConfiguration();
    }

    @Test
    void setScientificLocale() {
        NumberFormatConfig.formatPattern = "SCIENTIFIC";
        NumberFormatUtil.postConfiguration();

        double value = 1_888_888_888_888L;

        Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
        assertEquals("1,89e12", NumberFormatUtil.formatNumber(value));
        assertEquals("1,89e12", NumberFormatUtil.formatNumberCompact(value));
    }

    @Test
    void scientificFormat_formatsUsingExponent() {
        NumberFormatConfig.formatPattern = "SCIENTIFIC";
        NumberFormatUtil.postConfiguration();

        assertEquals("1.23e12", NumberFormatUtil.formatNumber(VALUE));
        assertEquals("1.23e13", NumberFormatUtil.formatNumber(VALUE * 10));
        assertEquals("1.23e14", NumberFormatUtil.formatNumber(VALUE * 100));

        assertEquals("-1.23e12", NumberFormatUtil.formatNumber(-VALUE));
        assertEquals("-1.23e13", NumberFormatUtil.formatNumber(-VALUE * 10));
        assertEquals("-1.23e14", NumberFormatUtil.formatNumber(-VALUE * 100));
    }

    @Test
    void engineeringFormat_formatsUsingExponent() {
        NumberFormatConfig.formatPattern = "ENGINEERING";
        NumberFormatUtil.postConfiguration();

        assertEquals("1.23e12", NumberFormatUtil.formatNumber(VALUE));
        assertEquals("12.35e12", NumberFormatUtil.formatNumber(VALUE * 10));
        assertEquals("123.46e12", NumberFormatUtil.formatNumber(VALUE * 100));
    }

    @Test
    void powerOfTenFormat_formatsUsingExplicitPower() {
        NumberFormatConfig.formatPattern = "POWER_OF_TEN";
        NumberFormatUtil.postConfiguration();

        assertEquals("1.23*10^12", NumberFormatUtil.formatNumber(VALUE));
        assertEquals("1.23*10^13", NumberFormatUtil.formatNumber(VALUE * 10));
        assertEquals("1.23*10^14", NumberFormatUtil.formatNumber(VALUE * 100));

        assertEquals("-1.23*10^12", NumberFormatUtil.formatNumber(-VALUE));
        assertEquals("-1.23*10^13", NumberFormatUtil.formatNumber(-VALUE * 10));
        assertEquals("-1.23*10^14", NumberFormatUtil.formatNumber(-VALUE * 100));
    }

    @Test
    void scientificFormat_roundingBehavior() {
        NumberFormatConfig.formatPattern = "SCIENTIFIC";
        NumberFormatUtil.postConfiguration();

        assertEquals("1.23e12", NumberFormatUtil.formatNumber(1.234e12));
        assertEquals("1.24e12", NumberFormatUtil.formatNumber(1.235e12));
        assertEquals("1.24e12", NumberFormatUtil.formatNumber(1.239e12));

        assertEquals("-1.23e12", NumberFormatUtil.formatNumber(-1.234e12));
        assertEquals("-1.24e12", NumberFormatUtil.formatNumber(-1.235e12));
    }

    @Test
    void engineeringFormat_roundingBehavior() {
        NumberFormatConfig.formatPattern = "ENGINEERING";
        NumberFormatUtil.postConfiguration();

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
    void powerOfTenFormat_roundingBehavior() {
        NumberFormatConfig.formatPattern = "POWER_OF_TEN";
        NumberFormatUtil.postConfiguration();

        assertEquals("1.23*10^12", NumberFormatUtil.formatNumber(1.234e12));
        assertEquals("1.24*10^12", NumberFormatUtil.formatNumber(1.235e12));

        assertEquals("1.23*10^13", NumberFormatUtil.formatNumber(1.234e13));
        assertEquals("1.24*10^13", NumberFormatUtil.formatNumber(1.235e13));

        assertEquals("-1.23*10^12", NumberFormatUtil.formatNumber(-1.234e12));
        assertEquals("-1.24*10^12", NumberFormatUtil.formatNumber(-1.235e12));
    }

    @Test
    void engineeringFormat_rolloverOnRounding() {
        NumberFormatConfig.formatPattern = "ENGINEERING";
        NumberFormatUtil.postConfiguration();

        assertEquals("1e15", NumberFormatUtil.formatNumber(9.99995e14));
        assertEquals("10e15", NumberFormatUtil.formatNumber(9.99995e15));
        assertEquals("100e15", NumberFormatUtil.formatNumber(9.99995e16));
        assertEquals("1e18", NumberFormatUtil.formatNumber(9.99995e17));
    }

    @Test
    void engineeringFormat_locale() {
        NumberFormatConfig.formatPattern = "ENGINEERING";
        NumberFormatUtil.postConfiguration();

        Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
        assertEquals("11,11e24", NumberFormatUtil.formatNumber(1.1111111e25));
    }

    @Test
    void scientificFormat_cutOff() {
        NumberFormatConfig.formatPattern = "SCIENTIFIC";
        NumberFormatUtil.postConfiguration();

        long threshold = BD_TRILLION.longValue();

        assertEquals("999,999,999,999", NumberFormatUtil.formatNumber(threshold - 1));
        assertEquals("1e12", NumberFormatUtil.formatNumber(threshold));
        assertEquals("1e12", NumberFormatUtil.formatNumber(threshold + 1));
    }

    @Test
    void scientificThresholdOption() {
        NumberFormatConfig.formatPattern = "SCIENTIFIC";
        NumberFormatUtil.postConfiguration();

        // Safe thresholds
        FormatOptions formatOptions = new FormatOptions().setScientificThreshold(BD_TRILLION); // 1T
        CompactOptions compactOptions = new CompactOptions().setCompactThreshold(BD_THOUSAND) // 1K
                .setScientificThreshold(BD_TRILLION); // 1T

        // Verify formatting respects thresholds
        assertEquals("1e12", NumberFormatUtil.formatNumber(BD_TRILLION, formatOptions));
        assertEquals("1K", NumberFormatUtil.formatNumberCompact(BD_THOUSAND, compactOptions));
        assertEquals("1e12", NumberFormatUtil.formatNumberCompact(BD_TRILLION, compactOptions));

        // Defaults are safe: compact 1K < scientific 1T
        CompactOptions defaultOptions = new CompactOptions();
        assertTrue(defaultOptions.getCompactThreshold().compareTo(defaultOptions.getScientificThreshold()) < 0);

        // Unsafe: compact >= scientific should throw
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompactOptions().setScientificThreshold(500).setCompactThreshold(500));

        assertThrows(
                IllegalArgumentException.class,
                () -> new CompactOptions().setCompactThreshold(2 * BD_TRILLION.longValue()));
    }

    @Test
    void testScientificRoundingAcrossMagnitude() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        NumberFormatConfig.formatPattern = "SCIENTIFIC";
        NumberFormatUtil.postConfiguration();

        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);

            CompactOptions options = new CompactOptions();

            // CEILING crosses magnitude
            options.setRoundingMode(RoundingMode.CEILING);
            assertEquals("1.01e12", NumberFormatUtil.formatNumberCompact(1_000_000_000_001L, options));

            // HALF_UP rounds based on midpoint
            options.setRoundingMode(RoundingMode.HALF_UP);
            assertEquals("2e12", NumberFormatUtil.formatNumberCompact(1_995_000_000_001L, options));
            assertEquals("1.99e12", NumberFormatUtil.formatNumberCompact(1_990_000_000_001L, options));

        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
        }
    }

}
