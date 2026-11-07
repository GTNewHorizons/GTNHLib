package com.gtnewhorizon.gtnhlib.util.numberformatting;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_THOUSAND;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;

import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;

public enum ExponentialFormat {

    SCIENTIFIC {

        @Override
        protected @NotNull String getFormattingKey() {
            return NumberFormatUtil.LOC_CACHE.getExponentialFormatScientific();
        }
    },

    ENGINEERING {

        @Override
        protected @NotNull String getFormattingKey() {
            return NumberFormatUtil.LOC_CACHE.getExponentialFormatEngineering();
        }

        @Override
        @NotNull
        public ObjectIntImmutablePair<BigDecimal> getComponents(@NotNull BigDecimal value,
                @NotNull NumberOptionsBase<?> options) {
            // calc exponent
            BigDecimal abs = value.abs();
            int exponent = abs.precision() - 1 - abs.scale();
            exponent = Math.floorDiv(exponent, 3) * 3;

            // calc mantissa
            BigDecimal mantissa = value.movePointLeft(exponent)
                    .setScale(options.getMaxDecimalPlaces(), options.getRoundingMode());

            // rounding can push mantissa to 1000 -> re-normalise to keep engineering form
            BigDecimal absMantissa = mantissa.abs();
            if (absMantissa.compareTo(BD_THOUSAND) >= 0) {
                mantissa = mantissa.movePointLeft(3).setScale(options.getMaxDecimalPlaces(), options.getRoundingMode());
                exponent += 3;
            }

            // Strip trailing zeros
            mantissa = mantissa.stripTrailingZeros();
            return ObjectIntImmutablePair.of(mantissa, exponent);
        }
    },

    POWER_OF_TEN {

        @Override
        protected @NotNull String getFormattingKey() {
            return NumberFormatUtil.LOC_CACHE.getExponentialFormatPowerOfTen();
        }
    };

    String format(BigDecimal v, NumberOptionsBase<?> options) {
        ObjectIntImmutablePair<BigDecimal> components = this.getComponents(v, options);
        DecimalFormat df = getMantissaFormatter(options);
        return String.format(this.getFormattingKey(), df.format(components.left()), components.rightInt());
    }

    protected @NotNull String getFormattingKey() {
        return "%se%s";
    }

    /**
     * @implSpec The mantissa should always be zero-stripped
     */
    public @NotNull ObjectIntImmutablePair<BigDecimal> getComponents(@NotNull BigDecimal value,
            @NotNull NumberOptionsBase<?> options) {
        BigDecimal rounded = value.round(new MathContext(options.getMaxDecimalPlaces() + 1, options.getRoundingMode()));
        int exponent = rounded.precision() - 1 - rounded.scale();
        BigDecimal mantissa = rounded.movePointLeft(exponent).stripTrailingZeros();
        return ObjectIntImmutablePair.of(mantissa, exponent);
    }

    public static ExponentialFormat parse(String raw) {
        try {
            return ExponentialFormat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Invalid scientific format '" + raw + "'. Valid values: " + Arrays.toString(values()));
        }
    }

    private static DecimalFormat getMantissaFormatter(NumberOptionsBase<?> options) {
        Locale locale = NumberFormatConfig.getActiveLocale();
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);

        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(true);
        df.setRoundingMode(options.getRoundingMode());
        df.setMinimumFractionDigits(options.getMinDecimalPlaces());
        df.setMaximumFractionDigits(Math.max(1, options.getMaxDecimalPlaces()));
        return df;
    }
}
