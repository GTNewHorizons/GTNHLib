package com.gtnewhorizon.gtnhlib.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

public class NumberFormatUtilTest {

    private static void assertLocaleFormats(Locale locale, double[] inputs, String[] expected) {
        if (inputs.length != expected.length) {
            throw new IllegalArgumentException("inputs and expected must have same length");
        }

        Locale old = Locale.getDefault(Locale.Category.FORMAT);

        try {
            Locale.setDefault(Locale.Category.FORMAT, locale);
            NumberFormatUtil.resetForTests();

            for (int i = 0; i < inputs.length; i++) {
                double value = inputs[i];
                String formatted = NumberFormatUtil.formatNumbers(value);

                assertTrue(
                        expected[i].equals(formatted),
                        locale + " failed for " + value + ": " + formatted + " (expected " + expected[i] + ")");
            }

        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    // Actual tests (locale + variants)

    // spotless:off
    @Test
    void testUSLocaleFormatting() {
        assertLocaleFormats(
            Locale.US,
            new double[] {
                0,
                1,
                12.3,
                999.99,
                1234.56,
                1234.125,
                -1234.56,
                1_000_000.999,
                1_000_000.11111111,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            },
            new String[] {
                "0",
                "1",
                "12.3",
                "999.99",
                "1,234.56",
                "1,234.13",
                "-1,234.56",
                "1,000,001",
                "1,000,000.11",
                "NaN",
                "Infinity",
                "-Infinity"
            }
        );
    }

    @Test
    void testFrenchLocaleFormatting() {
        assertLocaleFormats(
            Locale.FRANCE,
            new double[] {
                0,
                1,
                12.3,
                999.99,
                1234.56,
                1234.125,
                -1234.56,
                1_000_000.999
            },
            new String[] {
                "0",
                "1",
                "12,3",
                "999,99",
                "1 234,56",
                "1 234,13",
                "-1 234,56",
                "1 000 001"
            }
        );
    }

    @Test
    void testRussianLocaleFormatting() {
        assertLocaleFormats(
            new Locale("ru", "RU"),
            new double[] {
                0,
                1,
                12.3,
                999.99,
                1234.56,
                1234.125,
                -1234.56,
                1_000_000.999
            },
            new String[] {
                "0",
                "1",
                "12,3",
                "999,99",
                "1 234,56",
                "1 234,13",
                "-1 234,56",
                "1 000 001"
            }
        );
    }

    @Test
    void testSwissGermanLocaleFormatting() {
        assertLocaleFormats(
            new Locale("de", "CH"),
            new double[] {
                0,
                1,
                12.3,
                999.99,
                1234.56,
                1234.125,
                -1234.56,
                1_000_000.999
            },
            new String[] {
                "0",
                "1",
                "12.3",
                "999.99",
                "1'234.56",
                "1'234.13",
                "-1'234.56",
                "1'000'001"
            }
        );
    }

    @Test
    void testScientificFormatting() {
        assertLocaleFormats(Locale.US,
            new double[] {
                Double.MAX_VALUE,
                -Double.MAX_VALUE,
                2_000_000_000_000.0,
                2_333_333_000_000.0,
            },
            new String[] {
                "1.8e308",
                "-1.8e308",
                "2e12",
                "2.33e12"
            }
        );
        // spotless:on
    }

    @Test
    void testFluidFormatting() {
        assertEquals("1,000,000 mB", NumberFormatUtil.formatFluid(1000000));
    }
}
