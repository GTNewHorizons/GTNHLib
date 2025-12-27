package com.gtnewhorizon.gtnhlib.util.numberformatting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

public enum ScientificFormat {

    SCIENTIFIC {

        @Override
        String format(BigDecimal v, int sig) {
            return formatScientificStandard(v, sig);
        }
    },

    // NOTE: Engineering format intentionally ignores significantDigits.
    // Engineering notation prioritizes scale (exponent in multiples of 3)
    // with a fixed decimal precision for readability, rather than a fixed
    // number of significant figures. This avoids mantissa instability and
    // rounding artifacts when values grow in magnitude.
    ENGINEERING {

        @Override
        String format(BigDecimal v, int ignored) {
            return formatScientificEngineering(v);
        }
    },

    POWER_OF_TEN {

        @Override
        String format(BigDecimal v, int sig) {
            return formatScientificPowerOfTen(v, sig);
        }
    };

    abstract String format(BigDecimal value, int significantDigits);

    public static ScientificFormat parse(String raw) {
        try {
            return ScientificFormat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Invalid scientific format '" + raw + "'. Valid values: " + Arrays.toString(values()));
        }
    }

    private static String formatScientificStandard(BigDecimal value, int significantDigits) {
        BigDecimal rounded = value.round(new MathContext(significantDigits, RoundingMode.HALF_UP));

        int fractionalDigits = Math.max(1, significantDigits - 1);

        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        symbols.setExponentSeparator("e");

        StringBuilder pattern = new StringBuilder("0.");
        for (int i = 0; i < fractionalDigits; i++) {
            pattern.append('#');
        }
        pattern.append("E0");

        DecimalFormat df = new DecimalFormat(pattern.toString(), symbols);
        df.setGroupingUsed(false);
        df.setRoundingMode(RoundingMode.HALF_UP);

        return df.format(rounded);
    }

    private static String formatScientificEngineering(BigDecimal value) {
        if (value.signum() == 0) return "0";

        // Work with absolute value, reapply sign at the end
        BigDecimal abs = value.abs();

        // Compute base-10 exponent from ORIGINAL value
        int exponent = abs.precision() - 1 - abs.scale();

        // Snap exponent to multiple of 3
        int engineeringExponent = exponent - Math.floorMod(exponent, 3);

        // Scale to engineering mantissa
        BigDecimal mantissa = abs.movePointLeft(engineeringExponent).setScale(2, RoundingMode.HALF_UP);

        // Handle rollover: 999.995 → 1000.00 → 1.00e(next)
        if (mantissa.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            mantissa = mantissa.movePointLeft(3).setScale(2, RoundingMode.HALF_UP);
            engineeringExponent += 3;
        }

        if (value.signum() < 0) {
            mantissa = mantissa.negate();
        }

        return mantissa.toPlainString() + "e" + engineeringExponent;
    }

    private static String formatScientificPowerOfTen(BigDecimal value, int significantDigits) {
        BigDecimal rounded = value.round(new MathContext(significantDigits, RoundingMode.HALF_UP));

        int exponent = rounded.precision() - 1 - rounded.scale();
        BigDecimal mantissa = rounded.movePointLeft(exponent);

        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);

        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(false);
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMaximumFractionDigits(Math.max(1, significantDigits - 1));

        return df.format(mantissa) + "*10^" + exponent;
    }
}
