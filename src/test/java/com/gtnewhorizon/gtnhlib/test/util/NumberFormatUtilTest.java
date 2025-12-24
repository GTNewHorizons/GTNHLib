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
                String formatted = NumberFormatUtil.formatNumber(inputs[i]);
                assertEquals(expected[i], formatted,
                    locale + " failed for " + inputs[i]);
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
    void testCompactFormattingDefaultThreshold() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals(
                "1.23M",
                NumberFormatUtil.formatNumberCompact(1_234_567)
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void abbreviationThresholdControlsWhenAbbreviationStarts() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions opts =
                new NumberFormatOptions()
                    .abbreviationThreshold(BigInteger.valueOf(10_000_000_000L));

            assertEquals(
                "1,234,567,890",
                NumberFormatUtil.formatNumberCompact(1_234_567_890L, opts)
            );

            assertEquals(
                "1.23B",
                NumberFormatUtil.formatNumberCompact(1_234_567_890L,
                    new NumberFormatOptions()
                        .abbreviationThreshold(BigInteger.valueOf(1_000_000L)))
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

            assertTrue(NumberFormatUtil.formatNumberCompact(huge).contains("e"));

            assertTrue(
                NumberFormatUtil.formatNumberCompact(
                    huge,
                    new NumberFormatOptions().abbreviationThreshold(BigInteger.valueOf(10))
                ).contains("e")
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
                new NumberFormatOptions().significantDigits(5);

            assertEquals(
                "1.2346M",
                NumberFormatUtil.formatNumberCompact(1_234_567, opts)
            );

            assertEquals(
                "1.2346e12",
                NumberFormatUtil.formatNumber(1_234_567_890_123L, opts)
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void formatFluidCompactHonoursThreshold() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals(
                "1.23M mB",
                NumberFormatUtil.formatFluidCompact(
                    1_234_567,
                    new NumberFormatOptions()
                        .abbreviationThreshold(BigInteger.valueOf(1_000_000))
                )
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void formatEnergyCompactHonoursThreshold() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals(
                "9.88M EU",
                NumberFormatUtil.formatEnergyCompact(
                    9_876_543,
                    new NumberFormatOptions()
                        .abbreviationThreshold(BigInteger.valueOf(1_000_000))
                )
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void negativeValuesAreAbbreviatedCorrectly() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
            NumberFormatUtil.resetForTests();

            assertEquals(
                "-1,23M",
                NumberFormatUtil.formatNumberCompact(
                    -1_234_567,
                    new NumberFormatOptions()
                        .abbreviationThreshold(BigInteger.valueOf(1_000_000))
                )
            );
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void zeroNeverUsesScientificNotation() {
        postConfiguration();

        assertEquals("0", NumberFormatUtil.formatNumber(0));
        assertEquals("0", NumberFormatUtil.formatNumberCompact(0));
        assertEquals("0", NumberFormatUtil.formatNumber(BigInteger.ZERO));
    }

    @Test
    void disableFormattedNotationBypassesFormatting() {
        boolean old = NumberFormatConfig.disableFormattedNotation;
        try {
            NumberFormatConfig.disableFormattedNotation = true;
            NumberFormatUtil.resetForTests();

            assertEquals("1000000", NumberFormatUtil.formatNumber(1_000_000));
            assertEquals("1000000",
                NumberFormatUtil.formatFluidCompact(1_000_000,
                    new NumberFormatOptions()).split(" ")[0]
            );
        } finally {
            NumberFormatConfig.disableFormattedNotation = old;
            NumberFormatUtil.resetForTests();
        }
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
}
