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

    private static final long DEFAULT_ABBREVIATION_THRESHOLD = 1_000_000_000_000L; // 1T
    private static final int SIGNIFICANT_DIGITS = 3;

    /* ========================= Formatters ========================= */

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(() -> {
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);

        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(true);
        df.setMaximumFractionDigits(decimalPlaces);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    });

    private static final ThreadLocal<DecimalFormat> SCIENTIFIC_FORMAT = ThreadLocal.withInitial(() -> {
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        symbols.setExponentSeparator("e");

        StringBuilder pattern = new StringBuilder("0.");
        for (int i = 0; i < scientificDecimalPlaces; i++) {
            pattern.append('#');
        }
        pattern.append("E0");

        DecimalFormat df = new DecimalFormat(pattern.toString(), symbols);
        df.setGroupingUsed(false);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    });

    private NumberFormatUtil() {}

    /* ========================= Numbers ========================= */

    public static String formatNumber(Number value) {
        return formatNumber(value, DEFAULT_ABBREVIATION_THRESHOLD);
    }

    public static String formatNumber(Number value, Number abbreviationThreshold) {
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
        BigDecimal threshold = toBigDecimal(abbreviationThreshold);

        if (abs.signum() == 0) return "0";

        // Plain formatting below threshold
        if (abs.compareTo(threshold) < 0) {
            return centralFormatter(FORMAT.get().format(val));
        }

        // Abbreviation for < 1T
        if (abs.compareTo(BD_TRILLION) < 0) {
            return abbreviate(val);
        }

        // Scientific for >= 1T
        return formatScientific(val);
    }

    /* ========================= Abbreviation ========================= */

    private static String abbreviate(BigDecimal value) {
        BigDecimal abs = value.abs();
        BigDecimal scaled;
        String suffix;

        if (abs.compareTo(BD_TRILLION) >= 0) {
            scaled = value.divide(BD_TRILLION, MathContext.UNLIMITED);
            suffix = "T";
        } else if (abs.compareTo(BD_BILLION) >= 0) {
            scaled = value.divide(BD_BILLION, MathContext.UNLIMITED);
            suffix = "B";
        } else if (abs.compareTo(BD_MILLION) >= 0) {
            scaled = value.divide(BD_MILLION, MathContext.UNLIMITED);
            suffix = "M";
        } else {
            scaled = value.divide(BD_THOUSAND, MathContext.UNLIMITED);
            suffix = "K";
        }

        BigDecimal rounded = scaled.round(new MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP));
        rounded = ensureFractionalPrecision(scaled, rounded);

        return stripTrailingZeros(rounded) + suffix;
    }

    /**
     * Significant-digit rounding can collapse fractional digits (e.g. 123.456 -> 123 with 3 sig figs), which is too
     * lossy for abbreviated display. If rounding removed all fractional digits but the original scaled value had them,
     * reintroduce a small amount of fractional precision derived from {@link #SIGNIFICANT_DIGITS} so values like
     * 123.456M become 123.46M.
     */
    private static BigDecimal ensureFractionalPrecision(BigDecimal originalScaled, BigDecimal rounded) {
        if (rounded.scale() > 0) return rounded;
        if (originalScaled.stripTrailingZeros().scale() <= 0) return rounded;

        int minDecimals = Math.max(2, SIGNIFICANT_DIGITS - 1);
        return originalScaled.setScale(minDecimals, RoundingMode.HALF_UP);
    }

    private static String stripTrailingZeros(BigDecimal bd) {
        return bd.stripTrailingZeros().toPlainString();
    }

    /* ========================= Scientific ========================= */

    private static String formatScientific(BigDecimal value) {
        return SCIENTIFIC_FORMAT.get().format(value.round(new MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP)));
    }

    /* ========================= Fluids ========================= */

    public static String getFluidUnit() {
        return useForgeFluidMillibuckets ? "mB" : "L";
    }

    public static String formatFluid(Number value) {
        return formatFluid(value, DEFAULT_ABBREVIATION_THRESHOLD);
    }

    public static String formatFluid(Number value, Number abbreviationThreshold) {
        return formatNumber(value, abbreviationThreshold) + " " + getFluidUnit();
    }

    public static String formatFluid(FluidStack stack) {
        return formatFluid(stack.amount);
    }

    /* ========================= Energy ========================= */

    public static String getEnergyUnit() {
        return "EU";
    }

    public static String formatEnergy(Number value) {
        return formatEnergy(value, DEFAULT_ABBREVIATION_THRESHOLD);
    }

    public static String formatEnergy(Number value, Number abbreviationThreshold) {
        return formatNumber(value, abbreviationThreshold) + " " + getEnergyUnit();
    }

    /* ========================= Internals ========================= */

    private static BigDecimal toBigDecimal(Number n) {
        if (n == null) return BigDecimal.ZERO;
        if (n instanceof BigDecimal bd) return bd;
        if (n instanceof BigInteger bi) return new BigDecimal(bi);
        if (n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long) {
            return BigDecimal.valueOf(n.longValue());
        }
        return BigDecimal.valueOf(n.doubleValue());
    }

    private static String centralFormatter(String s) {
        return s.replace("\u202F", " ").replace("\u00A0", " ").replace("\u2019", "'");
    }

    public static void postConfiguration() {
        resetForTests();
    }

    @VisibleForTesting
    public static void resetForTests() {
        FORMAT.remove();
        SCIENTIFIC_FORMAT.remove();
    }
}
