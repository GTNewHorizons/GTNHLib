package com.gtnewhorizon.gtnhlib.util.numberformatting;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_BILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_MILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_QUADRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_THOUSAND;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_TRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;

@SuppressWarnings("unused")
public final class NumberFormatUtil {

    /* ========================= Formatters ========================= */

    private NumberFormatUtil() {}

    /* ========================= Main Formatter ========================= */

    public static String formatNumber(Number value) {
        return formatNumber(value, new FormatOptions());
    }

    public static String formatNumber(Number value, FormatOptions options) {
        String special = handleSpecialCases(value);
        if (special != null) return special;

        BigDecimal val = bigDecimalConverter(value);
        BigDecimal abs = val.abs();

        if (abs.signum() == 0) return "0";

        // Scientific for more than 1T
        if (isScientificEnabled(abs, options)) {
            return formatScientific(val, options);
        }

        // Plain locale formatting
        return centralFormatter(getDefaultDecimalFormatter(options).format(val));
    }

    /* ========================= Compact Numbers ========================= */

    public static String formatNumberCompact(Number value) {
        return formatNumberCompact(value, new CompactOptions());
    }

    /**
     * Compact / abbreviated formatting.
     * <p>
     * Below the abbreviation threshold becomes standard formatting. Between threshold and 1T becomes abbreviated
     * (K/M/B), greater than 1T becomes scientific. Options objects can modify this behaviour.
     */
    public static String formatNumberCompact(Number value, CompactOptions options) {
        String special = handleSpecialCases(value);
        if (special != null) return special;

        BigDecimal val = bigDecimalConverter(value);
        BigDecimal abs = val.abs();

        if (abs.signum() == 0) return "0";

        // Below threshold, do exact formatting
        if (abs.compareTo(options.getCompactThreshold()) < 0) {
            return formatNumber(value, options.toFormatOptions());
        }

        // More than 1T, do scientific.
        if (isScientificEnabled(abs, options)) {
            return formatScientific(val, options);
        }

        // Abbreviated (1K, 2.5M etc)
        return abbreviate(val, options);
    }

    /* ========================= Abbreviation ========================= */

    private static String abbreviate(BigDecimal value, NumberOptionsBase<?> options) {
        BigDecimal abs = value.abs();

        BigDecimal divisor;
        String suffix;

        if (abs.compareTo(BD_QUADRILLION) >= 0) {
            divisor = BD_QUADRILLION;
            suffix = "Q";
        } else if (abs.compareTo(BD_TRILLION) >= 0) {
            divisor = BD_TRILLION;
            suffix = "T";
        } else if (abs.compareTo(BD_BILLION) >= 0) {
            divisor = BD_BILLION;
            suffix = "B";
        } else if (abs.compareTo(BD_MILLION) >= 0) {
            divisor = BD_MILLION;
            suffix = "M";
        } else {
            divisor = BD_THOUSAND;
            suffix = "K";
        }

        BigDecimal scaled = value.divide(divisor, MathContext.UNLIMITED);

        int dp = options.getDecimalPlaces();
        RoundingMode rm = options.getRoundingMode();

        BigDecimal rounded = scaled.setScale(dp, rm);

        // Prevent unit rollover caused only by rounding:
        // If abs(value) is still below the next threshold, do not allow rounded to reach 1000.
        BigDecimal nextThreshold = divisor.multiply(BD_THOUSAND);
        if (abs.compareTo(nextThreshold) < 0 && rounded.abs().compareTo(BD_THOUSAND) >= 0) {
            BigDecimal step = BigDecimal.ONE.scaleByPowerOfTen(-dp); // 10^-dp (works for dp <= 0 too)
            BigDecimal maxAbs = BD_THOUSAND.subtract(step).setScale(dp, RoundingMode.UNNECESSARY);
            rounded = (rounded.signum() < 0) ? maxAbs.negate() : maxAbs;
        }

        rounded = rounded.stripTrailingZeros();

        return formatNumber(rounded, options.toFormatOptions()) + suffix;
    }

    /* ========================= Scientific ========================= */

    private static String formatScientific(BigDecimal value, NumberOptionsBase<?> options) {
        return SCIENTIFIC_FORMAT.format(value, options);
    }

    /* ========================= Fluids ========================= */

    public static String getFluidUnit() {
        return useForgeFluidMillibuckets ? "mB" : "L";
    }

    public static String formatFluid(Number value) {
        return formatFluid(value, new FormatOptions());
    }

    public static String formatFluid(Number value, FormatOptions options) {
        return formatNumber(value, options) + " " + getFluidUnit();
    }

    public static String formatFluidCompact(Number value) {
        return formatFluidCompact(value, new CompactOptions());
    }

    public static String formatFluidCompact(Number value, CompactOptions options) {
        return formatNumberCompact(value, options) + " " + getFluidUnit();
    }

    public static String formatFluid(FluidStack stack) {
        return formatFluid(stack.amount);
    }

    /* ========================= Energy ========================= */

    public static String getEnergyUnit() {
        return "EU";
    }

    public static String formatEnergy(Number value) {
        return formatEnergy(value, new FormatOptions());
    }

    public static String formatEnergy(Number value, FormatOptions options) {
        return formatNumber(value, options) + " " + getEnergyUnit();
    }

    public static String formatEnergyCompact(Number value, CompactOptions options) {
        return formatNumberCompact(value, options) + " " + getEnergyUnit();
    }

    public static String formatEnergyCompact(Number value) {
        return formatEnergyCompact(value, new CompactOptions());
    }

    /* ========================= Internals ========================= */

    private static String rawNumberToString(Number value) {
        if (value instanceof BigDecimal bd) {
            // It seems BigDecimal.toString() may emit scientific notation, but
            // toPlainString() never does.
            return bd.toPlainString();
        }

        if (value instanceof BigInteger) {
            return value.toString();
        }

        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            // toPlainString avoids exponent notation.
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }

        // Common wrappers
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return Long.toString(value.longValue());
        }

        // Fallback: best effort was made!
        return value.toString();
    }

    private static boolean isScientificEnabled(BigDecimal absValue, NumberOptionsBase<?> options) {
        // Global flag disables scientific formatting entirely
        boolean disabledGlobally = globalDisableScientific;

        // Whether this specific number qualifies for scientific formatting
        boolean aboveThreshold = absValue.compareTo(options.getScientificThreshold()) >= 0;

        // Whether scientific formatting is enabled for this option
        boolean enabledInOptions = options.isScientificFormattingEnabled();

        // Scientific formatting is enabled only if all conditions are true
        return !disabledGlobally && enabledInOptions && aboveThreshold;
    }

    private static DecimalFormat getDefaultDecimalFormatter(NumberOptionsBase<?> options) {
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(true);
        df.setMaximumFractionDigits(options.getDecimalPlaces());
        df.setRoundingMode(options.getRoundingMode());

        return df;
    }

    private static String handleSpecialCases(Number value) {
        if (value == null) return "NULL";
        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            if (Double.isNaN(d)) return "NaN";
            if (Double.isInfinite(d)) return d > 0 ? "Infinity" : "-Infinity";
        }

        if (disableFormattedNotation) return rawNumberToString(value);

        return null; // Returning null signals no special case needed.
    }

    private static String centralFormatter(String s) {
        s = s.replace("\u202F", " ");
        s = s.replace("\u00A0", " ");
        return s;
    }

    public static void postConfiguration() {
        SCIENTIFIC_FORMAT = ScientificFormat.parse(formatPattern);
    }

    public static BigDecimal bigDecimalConverter(Number number) {
        if (number == null) {
            throw new IllegalArgumentException("Number cannot be null");
        }

        if (number instanceof BigDecimal bd) {
            return bd;
        }

        if (number instanceof BigInteger bi) {
            return new BigDecimal(bi);
        }

        // Preserve exact integer values (avoids > 2^53 precision issues)
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        }

        // Fallback: floating-point conversion
        double d = number.doubleValue();
        if (Double.isNaN(d)) {
            throw new IllegalArgumentException("Cannot convert NaN to BigDecimal.");
        }
        if (Double.isInfinite(d)) {
            throw new IllegalArgumentException("Cannot convert Infinity to BigDecimal.");
        }

        return BigDecimal.valueOf(d);
    }

}
