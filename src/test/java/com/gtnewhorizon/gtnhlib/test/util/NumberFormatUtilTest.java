package com.gtnewhorizon.gtnhlib.test.util;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.postConfiguration;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Locale;

import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentNumber;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;
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
                String formatted = NumberFormatUtil.formatNumber(value);

                assertEquals(
                        expected[i],
                        formatted,
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

    @Test
    void roundingIsHalfUp() {
        assertEquals("1.01", NumberFormatUtil.formatNumber(1.005));
    }

    @Test
    void bigIntegerThresholdUsesScientific() {
        BigInteger big = BigInteger.valueOf(10).pow(15);
        String s = NumberFormatUtil.formatNumber(big);
        assertTrue(s.contains("e"));
    }

    @Test
    void createCopyIsDeepEnough() {
        ChatComponentNumber original = new ChatComponentNumber(123);
        original.getChatStyle().setBold(true);
        original.appendSibling(new ChatComponentNumber(456));

        IChatComponent copy = original.createCopy();

        assertEquals(original, copy);
        assertNotSame(original, copy);
        assertNotSame(original.getSiblings().get(0), copy.getSiblings().get(0));
    }

    @Test
    void scientificThresholdBoundary() {
        double t = NumberFormatConfig.scientificThreshold;
        assertFalse(NumberFormatUtil.formatNumber(t - 1).contains("e"));
        assertTrue(NumberFormatUtil.formatNumber(t).contains("e"));
        assertTrue(NumberFormatUtil.formatNumber(t + 1).contains("e"));
    }

    @Test
    void disableFormattedNotationBypassesFormatting() {
        boolean old = NumberFormatConfig.disableFormattedNotation;
        try {
            NumberFormatConfig.disableFormattedNotation = true;
            NumberFormatUtil.resetForTests();

            assertEquals("1000000", NumberFormatUtil.formatNumber(1_000_000));
            assertEquals("1.005", NumberFormatUtil.formatNumber(1.005));
            assertEquals("1000000", NumberFormatUtil.formatFluid(1_000_000).split(" ")[0]);
        } finally {
            NumberFormatConfig.disableFormattedNotation = old;
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void negativeScientificThresholdUsesScientific() {
        double t = NumberFormatConfig.scientificThreshold;
        assertTrue(NumberFormatUtil.formatNumber(-t).contains("e"));
    }

    @Test
    void chatComponentNormalisesIntegerTypes() {
        assertEquals(new ChatComponentNumber(123L), new ChatComponentNumber(Integer.valueOf(123)));
    }

    @Test
    void zeroNeverUsesScientificNotation() {
        postConfiguration(); // Calculates big int scientific threshold.

        assertEquals("0", NumberFormatUtil.formatNumber(0));
        assertEquals("0", NumberFormatUtil.formatNumber(0.0));
        assertEquals("0", NumberFormatUtil.formatNumber(BigInteger.ZERO));
    }

}
