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

    private static final BigInteger THOUSAND = BigInteger.valueOf(1_000);
    private static final BigInteger MILLION  = BigInteger.valueOf(1_000_000);
    private static final BigInteger BILLION  = BigInteger.valueOf(1_000_000_000);
    private static final BigInteger TRILLION = BigInteger.valueOf(1_000_000_000_000L);

    private static BigInteger scientificThresholdBigInt = BigInteger.ZERO;

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

    /*
     * ========================= Public API =========================
     */

    private final static int abbreviationThreshold = 12;
    private final static int preferredSignificantDigits = 3;

    public static String formatNumber(long value) {
        return formatNumber(BigInteger.valueOf(value), abbreviationThreshold, preferredSignificantDigits);
    }

    public static String formatNumber(long value, int abbreviationThreshold) {
        return formatNumber(BigInteger.valueOf(value), abbreviationThreshold, preferredSignificantDigits);
    }

    public static String formatNumber(long value, int abbreviationThreshold, int preferredSignificantDigits) {
        return formatNumber(BigInteger.valueOf(value), abbreviationThreshold, preferredSignificantDigits);
    }

    public static String formatNumber(BigInteger value) {
        return formatNumber(value, abbreviationThreshold, preferredSignificantDigits);
    }

    public static String formatNumber(BigInteger value, int abbreviationThreshold) {
        return formatNumber(value, abbreviationThreshold, preferredSignificantDigits);
    }

    public static String formatNumber(BigInteger value, int abbreviationThreshold, int preferredSignificantDigits) {
        if (disableFormattedNotation) return value.toString();
        if (value.signum() == 0) return "0";

        BigInteger abs = value.abs();
        int digits = abs.toString().length();

        if (digits <= abbreviationThreshold) {
            return centralFormatter(FORMAT.get().format(value));
        }

        if (abs.compareTo(TRILLION) < 0) {
            return abbreviate(new BigDecimal(value), preferredSignificantDigits);
        }

        return formatScientific(value, preferredSignificantDigits);
    }

    public static String formatNumber(double value) {
        return formatNumber(value, abbreviationThreshold, preferredSignificantDigits);
    }

    public static String formatNumber(double value, int abbreviationThreshold) {
        return formatNumber(value, abbreviationThreshold, preferredSignificantDigits);
    }

    public static String formatNumber(double value, int abbreviationThreshold, int preferredSignificantDigits) {
        if (disableFormattedNotation) return String.valueOf(value);
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";

        double abs = Math.abs(value);
        int digits = BigDecimal.valueOf((long) abs).toString().length();

        if (digits <= abbreviationThreshold) {
            return centralFormatter(FORMAT.get().format(value));
        }

        if (abs < 1_000_000_000_000L) {
            return abbreviate(BigDecimal.valueOf(value), preferredSignificantDigits);
        }

        return formatScientific(value, preferredSignificantDigits);
    }

    /*
     * ========================= Abbreviation =========================
     */

    private static String abbreviate(BigDecimal value, int sigDigits) {
        BigDecimal abs = value.abs();
        BigDecimal scaled;
        String suffix;

        if (abs.compareTo(new BigDecimal(TRILLION)) >= 0) {
            scaled = value.divide(new BigDecimal(TRILLION));
            suffix = "T";
        } else if (abs.compareTo(new BigDecimal(BILLION)) >= 0) {
            scaled = value.divide(new BigDecimal(BILLION));
            suffix = "B";
        } else if (abs.compareTo(new BigDecimal(MILLION)) >= 0) {
            scaled = value.divide(new BigDecimal(MILLION));
            suffix = "M";
        } else {
            scaled = value.divide(new BigDecimal(THOUSAND));
            suffix = "K";
        }

        MathContext mc = new MathContext(Math.max(1, sigDigits), RoundingMode.HALF_UP);
        scaled = scaled.round(mc);

        return stripTrailingZeros(scaled) + suffix;
    }

    private static String stripTrailingZeros(BigDecimal bd) {
        return bd.stripTrailingZeros().toPlainString();
    }

    /*
     * ========================= Scientific =========================
     */

    private static String formatScientific(BigInteger value, int sigDigits) {
        return formatScientific(new BigDecimal(value), sigDigits);
    }

    private static String formatScientific(double value, int sigDigits) {
        return formatScientific(BigDecimal.valueOf(value), sigDigits);
    }

    private static String formatScientific(BigDecimal value, int sigDigits) {
        MathContext mc = new MathContext(Math.max(1, sigDigits), RoundingMode.HALF_UP);
        BigDecimal rounded = value.round(mc);
        return SCIENTIFIC_FORMAT.get().format(rounded);
    }

    /*
     * ========================= Fluids =========================
     */

    public static String getFluidUnit() {
        return useForgeFluidMillibuckets ? "mB" : "L";
    }

    public static String formatFluid(long value) {
        return formatNumber(value) + " " + getFluidUnit();
    }

    public static String formatFluid(BigInteger value) {
        return formatNumber(value) + " " + getFluidUnit();
    }

    public static String formatFluid(double value) {
        return formatNumber(value) + " " + getFluidUnit();
    }

    public static String formatFluid(FluidStack stack) {
        return formatFluid(stack.amount);
    }

    /*
     * ========================= Energy =========================
     */

    public static String getEnergyUnit() {
        return "EU";
    }

    public static String formatEnergy(long value) {
        return formatNumber(value) + " " + getEnergyUnit();
    }

    public static String formatEnergy(BigInteger value) {
        return formatNumber(value) + " " + getEnergyUnit();
    }

    public static String formatEnergy(double value) {
        return formatNumber(value) + " " + getEnergyUnit();
    }

    /*
     * ========================= Internals =========================
     */

    private static String centralFormatter(String s) {
        return s.replace("\u202F", " ")
                .replace("\u00A0", " ")
                .replace("\u2019", "'");
    }

    public static void postConfiguration() {
        scientificThresholdBigInt = BigDecimal.valueOf(scientificThreshold)
                .setScale(0, RoundingMode.HALF_UP)
                .toBigIntegerExact();
        resetForTests();
    }

    @VisibleForTesting
    public static void resetForTests() {
        FORMAT.remove();
        SCIENTIFIC_FORMAT.remove();
    }
}
