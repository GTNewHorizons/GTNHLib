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

    /* ========================= Rollover =============================== */

    @Test
    void abbreviationRolloverPromotesSuffix() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions opts = new NumberFormatOptions().significantDigits(2);

            assertEquals("1M", NumberFormatUtil.formatNumberCompact(999_500, opts));
            assertEquals("1B", NumberFormatUtil.formatNumberCompact(999_500_000, opts));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    /* ========================= Locale / Plain ========================= */

    @Test
    void usLocalePlainFormatting() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals("12.3", NumberFormatUtil.formatNumber(12.3));
            assertEquals("1,234.56", NumberFormatUtil.formatNumber(1234.56));
            assertEquals("1,234.13", NumberFormatUtil.formatNumber(1234.125));
            assertEquals("-1,234.56", NumberFormatUtil.formatNumber(-1234.56));
            assertEquals("1,000,001", NumberFormatUtil.formatNumber(1_000_000.999));

            assertEquals("NaN", NumberFormatUtil.formatNumber(Double.NaN));
            assertEquals("Infinity", NumberFormatUtil.formatNumber(Double.POSITIVE_INFINITY));
            assertEquals("-Infinity", NumberFormatUtil.formatNumber(Double.NEGATIVE_INFINITY));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void frenchLocalePlainFormatting() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
            NumberFormatUtil.resetForTests();

            assertEquals("12,3", NumberFormatUtil.formatNumber(12.3));
            assertEquals("1 234,56", NumberFormatUtil.formatNumber(1234.56));
            assertEquals("1 234,13", NumberFormatUtil.formatNumber(1234.125));
            assertEquals("-1 234,56", NumberFormatUtil.formatNumber(-1234.56));
            assertEquals("1 000 001", NumberFormatUtil.formatNumber(1_000_000.999));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    /* ========================= Compact / Abbreviation ========================= */

    @Test
    void compactFormattingHonoursThreshold() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions opts = new NumberFormatOptions().abbreviationThreshold(BigInteger.valueOf(1_000_000));

            assertEquals("1.23M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));
            assertEquals("-1M", NumberFormatUtil.formatNumberCompact(-1_000_000, opts));
            assertEquals("999,999", NumberFormatUtil.formatNumberCompact(999_999, opts));
            assertEquals("999", NumberFormatUtil.formatNumberCompact(999, opts));
            assertEquals("0", NumberFormatUtil.formatNumberCompact(0, opts));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void abbreviationRespectsLocaleDecimalSeparator() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
            NumberFormatUtil.resetForTests();

            assertEquals("1,23M", NumberFormatUtil.formatNumberCompact(1_234_567));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void bigIntegerCompactFormattingMatchesLong() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            assertEquals(
                    NumberFormatUtil.formatNumberCompact(1_234_567L),
                    NumberFormatUtil.formatNumberCompact(new BigInteger("1234567")));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    /* ========================= Significant Digits ========================= */

    @Test
    void significantDigitsImpact() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions opts = new NumberFormatOptions();

            opts.significantDigits(1);
            assertEquals("1M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

            opts.significantDigits(2);
            assertEquals("1.2M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

            opts.significantDigits(3);
            assertEquals("1.23M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

            opts.significantDigits(4);
            assertEquals("1.235M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

            opts.significantDigits(5);
            assertEquals("1.2346M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

            opts.significantDigits(6);
            assertEquals("1.23457M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

            opts.significantDigits(7);
            assertEquals("1.234567M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

            opts.significantDigits(8);
            assertEquals("1.234567M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

            opts.significantDigits(9);
            assertEquals("1.234567M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void significantDigitsSillyValues() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions opts = new NumberFormatOptions();

            assertThrows(
                IllegalArgumentException.class,
                () -> opts.significantDigits(-1)
            );

            assertThrows(
                IllegalArgumentException.class,
                () -> opts.significantDigits(0)
            );

            opts.significantDigits(Integer.MAX_VALUE);
            assertEquals("1.234567M", NumberFormatUtil.formatNumberCompact(1_234_567, opts));

        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    @Test
    void significantDigitsNeverAffectPlainIntegersOrDecimals() {
        Locale old = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            NumberFormatUtil.resetForTests();

            NumberFormatOptions opts = new NumberFormatOptions().significantDigits(1);

            assertEquals("1,234.13", NumberFormatUtil.formatNumber(1234.125, opts));
            assertEquals("1,234,567", NumberFormatUtil.formatNumber(1_234_567, opts));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, old);
            NumberFormatUtil.resetForTests();
        }
    }

    /* ========================= Zero / Edge ========================= */

    @Test
    void zeroIsAlwaysRenderedAsZero() {
        postConfiguration();

        assertEquals("0", NumberFormatUtil.formatNumber(0));
        assertEquals("0", NumberFormatUtil.formatNumber(-0.0));
        assertEquals("0", NumberFormatUtil.formatNumberCompact(0));
        assertEquals("0", NumberFormatUtil.formatNumber(BigInteger.ZERO));
    }

    /* ========================= Disable Formatting ========================= */

    @Test
    void disableFormattedNotationBypassesAllFormatting() {
        boolean old = NumberFormatConfig.disableFormattedNotation;
        try {
            NumberFormatConfig.disableFormattedNotation = true;
            NumberFormatUtil.resetForTests();

            assertEquals("1000000", NumberFormatUtil.formatNumber(1_000_000));
            assertEquals(
                    "1000000",
                    NumberFormatUtil.formatFluidCompact(1_000_000, new NumberFormatOptions()).split(" ")[0]);
            assertEquals(String.valueOf(1_000_000_000_000L), NumberFormatUtil.formatNumber(1_000_000_000_000L));
        } finally {
            NumberFormatConfig.disableFormattedNotation = old;
            NumberFormatUtil.resetForTests();
        }
    }

    /* ========================= ChatComponent ========================= */

    @Test
    void createCopyProducesDeepClone() {
        ChatComponentNumber original = new ChatComponentNumber(123);
        original.getChatStyle().setBold(true);
        original.appendSibling(new ChatComponentNumber(456));

        IChatComponent copy = original.createCopy();

        assertEquals(original, copy);
        assertNotSame(original, copy);
        assertNotSame(original.getSiblings().get(0), copy.getSiblings().get(0));
    }
}
