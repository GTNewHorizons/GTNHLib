package com.gtnewhorizon.gtnhlib.test.util;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

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
        formatPattern = oldFormatPattern;
        Locale.setDefault(Locale.Category.FORMAT, oldLocale);
        NumberFormatUtil.postConfiguration();
    }

    @Test
    void scientificFormat_formatsUsingExponent() {
        formatPattern = "SCIENTIFIC";
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
        formatPattern = "ENGINEERING";
        NumberFormatUtil.postConfiguration();

        assertEquals("1.23e12", NumberFormatUtil.formatNumber(VALUE));
        assertEquals("12.35e12", NumberFormatUtil.formatNumber(VALUE * 10));
        assertEquals("123.46e12", NumberFormatUtil.formatNumber(VALUE * 100));
    }

    @Test
    void powerOfTenFormat_formatsUsingExplicitPower() {
        formatPattern = "POWER_OF_TEN";
        NumberFormatUtil.postConfiguration();

        assertEquals("1.23*10^12", NumberFormatUtil.formatNumber(VALUE));
        assertEquals("1.23*10^13", NumberFormatUtil.formatNumber(VALUE * 10));
        assertEquals("1.23*10^14", NumberFormatUtil.formatNumber(VALUE * 100));

        assertEquals("-1.23*10^12", NumberFormatUtil.formatNumber(-VALUE));
        assertEquals("-1.23*10^13", NumberFormatUtil.formatNumber(-VALUE * 10));
        assertEquals("-1.23*10^14", NumberFormatUtil.formatNumber(-VALUE * 100));
    }
}
