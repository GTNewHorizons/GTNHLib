package com.gtnewhorizon.gtnhlib.util.numberformatting;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.VisibleForTesting;

@SuppressWarnings("unused")
public final class NumberFormatUtil {

    /* ========================= Constants ========================= */

    private static final BigDecimal BD_THOUSAND = BigDecimal.valueOf(1_000);
    private static final BigDecimal BD_MILLION = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal BD_BILLION = BigDecimal.valueOf(1_000_000_000);
    private static final BigDecimal BD_TRILLION = BigDecimal.valueOf(1_000_000_000_000L);

    private static final int DEFAULT_SIG_DIGITS = 3;
    private static final BigInteger DEFAULT_ABBREV_THRESHOLD = BigInteger.valueOf(1_000);

    /* ========================= Formatters ========================= */

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(() -> {
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);

        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(true);

        // Plain formatting contract
        df.setMaximumFractionDigits(2);
        df.setRoundingMode(RoundingMode.HALF_UP);

        return df;
    });

    private static final ThreadLocal<DecimalFormat> ABBREVIATED_FORMAT = ThreadLocal.withInitial(() -> {
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);

        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(false);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    });

    private NumberFormatUtil() {}

    /* ========================= Main Formatter ========================= */

    public static String formatNumber(Number value) {
        return formatNumber(value, new NumberFormatOptions());
    }

    /**
     * Canonical formatting.
     * <p>
     * Locale grouping is always exact. Scientific notation is used at ≥ 1T. Significant digits apply only to scientific
     * output.
     */
    public static String formatNumber(Number value, NumberFormatOptions options) {
        if (value == null) return "NULL";
        if (disableFormattedNotation) return String.valueOf(value);

        // Floating-point edge cases
        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            if (Double.isNaN(d)) return "NaN";
            if (Double.isInfinite(d)) return d > 0 ? "Infinity" : "-Infinity";
        }

        BigDecimal val = toBigDecimal(value);
        BigDecimal abs = val.abs();

        if (abs.signum() == 0) return "0";

        int sigDigits = options.getSignificantDigits() != null ? options.getSignificantDigits() : DEFAULT_SIG_DIGITS;

        // Scientific for ≥ 1T
        if (abs.compareTo(BD_TRILLION) >= 0) {
            return formatScientific(val, sigDigits);
        }

        // Plain locale formatting (never rounded)
        return centralFormatter(FORMAT.get().format(val));
    }

    /* ========================= Compact Numbers ========================= */

    public static String formatNumberCompact(Number value) {
        return formatNumberCompact(value, new NumberFormatOptions());
    }

    /**
     * Compact / abbreviated formatting.
     * <p>
     * Below the abbreviation threshold → plain formatting. Between threshold and 1T → abbreviated (K/M/B). ≥ 1T →
     * scientific.
     */
    public static String formatNumberCompact(Number value, NumberFormatOptions options) {
        if (value == null) return "NULL";
        if (disableFormattedNotation) return String.valueOf(value);

        BigDecimal val = toBigDecimal(value);
        BigDecimal abs = val.abs();

        if (abs.signum() == 0) return "0";

        int sigDigits = options.getSignificantDigits() != null ? options.getSignificantDigits() : DEFAULT_SIG_DIGITS;

        BigInteger threshold = options.getAbbreviationThreshold() != null ? options.getAbbreviationThreshold()
                : DEFAULT_ABBREV_THRESHOLD;

        BigDecimal bdThreshold = new BigDecimal(threshold);

        // Below threshold → exact formatting
        if (abs.compareTo(bdThreshold) < 0) {
            return centralFormatter(FORMAT.get().format(val));
        }

        // ≥ 1T → scientific
        if (abs.compareTo(BD_TRILLION) >= 0) {
            return formatScientific(val, sigDigits);
        }

        // Abbreviated
        return abbreviate(val, sigDigits);
    }

    /* ========================= Abbreviation ========================= */

    private static String abbreviate(BigDecimal value, int significantDigits) {
        BigDecimal abs = value.abs();
        BigDecimal scaled;
        String suffix;

        if (abs.compareTo(BD_BILLION) >= 0) {
            scaled = value.divide(BD_BILLION, MathContext.UNLIMITED);
            suffix = "B";
        } else if (abs.compareTo(BD_MILLION) >= 0) {
            scaled = value.divide(BD_MILLION, MathContext.UNLIMITED);
            suffix = "M";
        } else {
            scaled = value.divide(BD_THOUSAND, MathContext.UNLIMITED);
            suffix = "K";
        }

        BigDecimal rounded = scaled.round(new MathContext(significantDigits, RoundingMode.HALF_UP));

        rounded = ensureFractionalPrecision(scaled, rounded, significantDigits);

        DecimalFormat df = ABBREVIATED_FORMAT.get();
        df.setMaximumFractionDigits(rounded.scale());
        df.setMinimumFractionDigits(Math.max(0, rounded.scale() - rounded.stripTrailingZeros().scale()));

        return df.format(rounded) + suffix;
    }

    private static BigDecimal ensureFractionalPrecision(BigDecimal originalScaled, BigDecimal rounded,
            int significantDigits) {
        if (rounded.scale() > 0) return rounded;
        if (originalScaled.stripTrailingZeros().scale() <= 0) return rounded;

        int minDecimals = Math.max(2, significantDigits - 1);
        return originalScaled.setScale(minDecimals, RoundingMode.HALF_UP);
    }

    /* ========================= Scientific ========================= */

    private static String formatScientific(BigDecimal value, int significantDigits) {
        return SCIENTIFIC_FORMAT.format(value, significantDigits);
    }

    /* ========================= Fluids ========================= */

    public static String getFluidUnit() {
        return useForgeFluidMillibuckets ? "mB" : "L";
    }

    public static String formatFluid(Number value) {
        return formatFluid(value, new NumberFormatOptions());
    }

    public static String formatFluid(Number value, NumberFormatOptions options) {
        return formatNumber(value, options) + " " + getFluidUnit();
    }

    public static String formatFluidCompact(Number value, NumberFormatOptions options) {
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
        return formatEnergy(value, new NumberFormatOptions());
    }

    public static String formatEnergy(Number value, NumberFormatOptions options) {
        return formatNumber(value, options) + " " + getEnergyUnit();
    }

    public static String formatEnergyCompact(Number value, NumberFormatOptions options) {
        return formatNumberCompact(value, options) + " " + getEnergyUnit();
    }

    /* ========================= Internals ========================= */

    private static BigDecimal toBigDecimal(Number n) {
        if (n instanceof BigDecimal bd) return bd;
        if (n instanceof BigInteger bi) return new BigDecimal(bi);
        if (n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long) {
            return BigDecimal.valueOf(n.longValue());
        }
        return BigDecimal.valueOf(n.doubleValue());
    }

    private static String centralFormatter(String s) {
        s = s.replace("\u202F", " ");
        s = s.replace("\u00A0", " ");
        s = s.replace("\u00A1", " ");
        return s;
    }

    public static void postConfiguration() {
        SCIENTIFIC_FORMAT = ScientificFormat.parse(formatPattern);
        NumberFormatUtil.resetForTests();
    }

    @VisibleForTesting
    public static void resetForTests() {
        FORMAT.remove();
        ABBREVIATED_FORMAT.remove();
    }

}
