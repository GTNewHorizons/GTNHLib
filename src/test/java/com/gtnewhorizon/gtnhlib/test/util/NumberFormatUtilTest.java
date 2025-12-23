package com.gtnewhorizon.gtnhlib.test.util;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.postConfiguration;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Locale;

import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentNumber;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

public class NumberFormatUtilTest {

    private static void assertLocaleFormats(
        Locale locale,
        double[] inputs,
        String[] expected
    ) {
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
                    locale + " failed for " + value + ": " + formatted
                        + " (expected " + expected[i] + ")"
                );
            }
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

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
        assertLocaleFormats(
            Locale.US,
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
    }
    // spotless:on

    @Test
    void testFluidFormatting() {
        assertEquals("1,000,000 mB", NumberFormatUtil.formatFluid(1_000_000));
    }

    @Test
    void roundingIsHalfUp() {
        assertEquals("1.01", NumberFormatUtil.formatNumber(1.0050001));
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
    void disableFormattedNotationBypassesFormatting() {
        boolean old = NumberFormatConfig.disableFormattedNotation;
        try {
            NumberFormatConfig.disableFormattedNotation = true;
            NumberFormatUtil.resetForTests();

            assertEquals("1000000", NumberFormatUtil.formatNumber(1_000_000));
            assertEquals("1.005", NumberFormatUtil.formatNumber(1.005));
            assertEquals("1000000",
                NumberFormatUtil.formatFluid(1_000_000).split(" ")[0]
            );
        } finally {
            NumberFormatConfig.disableFormattedNotation = old;
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void chatComponentNormalisesIntegerTypes() {
        assertEquals(
            new ChatComponentNumber(123L),
            new ChatComponentNumber(Integer.valueOf(123))
        );
    }

    @Test
    void zeroNeverUsesScientificNotation() {
        postConfiguration();

        assertEquals("0", NumberFormatUtil.formatNumber(0));
        assertEquals("0", NumberFormatUtil.formatNumber(0.0));
        assertEquals("0", NumberFormatUtil.formatNumber(BigInteger.ZERO));
    }

    @Test
    void abbreviationThresholdControlsWhenAbbreviationStarts() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals(
                "1,234,567,890",
                NumberFormatUtil.formatNumber(1_234_567_890L)
            );

            assertEquals(
                "1.23B",
                NumberFormatUtil.formatNumber(
                    1_234_567_890L,
                    NumberFormatOptions.abbrev(BigInteger.valueOf(1_000_000_000L))
                )
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void abbreviationThresholdBoundaryIsInclusive() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            long value = 123_456_789L;

            assertEquals(
                "123.46M",
                NumberFormatUtil.formatNumber(
                    value,
                    NumberFormatOptions.abbrev(BigInteger.valueOf(value - 1))
                )
            );

            assertEquals(
                "123.46M",
                NumberFormatUtil.formatNumber(
                    value,
                    NumberFormatOptions.abbrev(BigInteger.valueOf(value))
                )
            );

            assertEquals(
                "123,456,789",
                NumberFormatUtil.formatNumber(
                    value,
                    NumberFormatOptions.abbrev(BigInteger.valueOf(value + 1))
                )
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void thresholdDoesNotAffectScientificCutover() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            double huge = 5_000_000_000_000.0;

            assertTrue(NumberFormatUtil.formatNumber(huge).contains("e"));

            assertTrue(
                NumberFormatUtil.formatNumber(
                    huge,
                    NumberFormatOptions.abbrev(BigInteger.valueOf(10_000))
                ).contains("e")
            );

            assertFalse(
                NumberFormatUtil.formatNumber(
                    huge,
                    NumberFormatOptions.abbrev(BigInteger.valueOf((long) huge + 1))
                ).contains("e")
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void formatFluidHonoursCustomAbbreviationThreshold() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals(
                "1,234,567 mB",
                NumberFormatUtil.formatFluid(1_234_567)
            );

            assertEquals(
                "1.23M mB",
                NumberFormatUtil.formatFluid(
                    1_234_567,
                    NumberFormatOptions.abbrev(BigInteger.valueOf(1_000_000))
                )
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void formatEnergyHonoursCustomAbbreviationThreshold() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals(
                "9,876,543 EU",
                NumberFormatUtil.formatEnergy(9_876_543)
            );

            assertEquals(
                "9.88M EU",
                NumberFormatUtil.formatEnergy(
                    9_876_543,
                    NumberFormatOptions.abbrev(BigInteger.valueOf(1_000_000))
                )
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void negativeValuesRespectCustomAbbreviationThreshold() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals(
                "-1.23M",
                NumberFormatUtil.formatNumber(
                    -1_234_567,
                    NumberFormatOptions.abbrev(BigInteger.valueOf(1_000_000))
                )
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void significantDigitsOverrideIsHonoured() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions opts =
                NumberFormatOptions.builder()
                    .significantDigits(5)
                    .abbreviationThreshold(BigInteger.valueOf(1_000))
                    .build();

            // Abbreviated path: sig-digits fully visible
            assertEquals(
                "1.2346M",
                NumberFormatUtil.formatNumber(1_234_567, opts)
            );

            // Scientific path: sig-digits rounded, but display capped by scientificDecimalPlaces
            assertEquals(
                "1.23e12",
                NumberFormatUtil.formatNumber(1_234_567_890_123L, opts)
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void abbreviatedNegativeValuesRespectLocale() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions opts =
                NumberFormatOptions.abbrev(BigInteger.valueOf(1_000_000));

            assertEquals(
                "-1,23M",
                NumberFormatUtil.formatNumber(-1_234_567, opts)
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void scientificPrecisionIsControlledBySignificantDigitsOnly() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions sig2 = NumberFormatOptions.sig(2);
            NumberFormatOptions sig6 = NumberFormatOptions.sig(6);

            double huge = Double.MAX_VALUE; // guaranteed scientific

            assertEquals(
                "1.8e308",
                NumberFormatUtil.formatNumber(huge, sig2)
            );

            assertEquals(
                "1.79769e308",
                NumberFormatUtil.formatNumber(huge, sig6)
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }
}
