package com.gtnewhorizon.gtnhlib.util.numberformatting;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_THOUSAND;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;

public enum ExponentialFormat {

    SCIENTIFIC {

        @Override
        String format(BigDecimal v, NumberOptionsBase<?> options) {
            return formatScientificStandard(v, options);
        }
    },

    ENGINEERING {

        @Override
        String format(BigDecimal v, NumberOptionsBase<?> options) {
            return formatScientificEngineering(v, options);
        }
    },

    POWER_OF_TEN {

        @Override
        String format(BigDecimal v, NumberOptionsBase<?> options) {
            return formatScientificPowerOfTen(v, options);
        }
    };

    abstract String format(BigDecimal value, NumberOptionsBase<?> options);

    public static ExponentialFormat parse(String raw) {
        try {
            return ExponentialFormat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Invalid scientific format '" + raw + "'. Valid values: " + Arrays.toString(values()));
        }
    }

    private static String formatScientificStandard(BigDecimal value, NumberOptionsBase<?> options) {
        BigDecimal rounded = value.round(new MathContext(options.getDecimalPlaces() + 1, options.getRoundingMode()));

        int fractionalDigits = Math.max(1, options.getDecimalPlaces());

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
        df.setRoundingMode(options.getRoundingMode());

        return df.format(rounded);
    }

    private static String formatScientificEngineering(BigDecimal value, NumberOptionsBase<?> options) {
        BigDecimal abs = value.abs();
        int base10Exponent = abs.precision() - 1 - abs.scale();
        int engExp = Math.floorDiv(base10Exponent, 3) * 3;

        BigDecimal mantissa = value.movePointLeft(engExp);
        mantissa = mantissa.setScale(options.getDecimalPlaces(), options.getRoundingMode());

        // rounding can push mantissa to 1000 -> re-normalise to keep engineering form
        BigDecimal absMantissa = mantissa.abs();
        if (absMantissa.compareTo(BD_THOUSAND) >= 0) {
            mantissa = mantissa.movePointLeft(3).setScale(options.getDecimalPlaces(), options.getRoundingMode());
            engExp += 3;
        }

        // Strip trailing zeros
        mantissa = mantissa.stripTrailingZeros();

        // Shove small number back through itself, recursive, but easier to handle.
        return formatNumber(mantissa, options.toFormatOptions()) + "e" + engExp;
    }

    private static String formatScientificPowerOfTen(BigDecimal value, NumberOptionsBase<?> options) {
        BigDecimal rounded = value.round(new MathContext(options.getDecimalPlaces() + 1, options.getRoundingMode()));

        int exponent = rounded.precision() - 1 - rounded.scale();
        BigDecimal mantissa = rounded.movePointLeft(exponent);

        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);

        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(false);
        df.setRoundingMode(options.getRoundingMode());
        df.setMaximumFractionDigits(Math.max(1, options.getDecimalPlaces()));

        return df.format(mantissa) + "*10^" + exponent;
    }
}
