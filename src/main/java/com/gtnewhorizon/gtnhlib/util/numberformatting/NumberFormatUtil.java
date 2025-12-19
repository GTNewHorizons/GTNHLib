package com.gtnewhorizon.gtnhlib.util.numberformatting;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.decimalPlaces;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.disableFormattedNotation;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.scientificDecimalPlaces;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.scientificThreshold;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.VisibleForTesting;

@SuppressWarnings("unused")
public final class NumberFormatUtil {

    private static BigInteger scientificThresholdBigInt = BigInteger.ZERO;

    // Feels overkill, but seems necessary in some contexts. See:
    // https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html
    // Synchronization:
    // "Decimal formats are generally not synchronized. It is recommended to create separate format instances for each
    // thread. If multiple threads access a format concurrently, it must be synchronized externally."
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

    // Ditto ThreadLocal reasoning, see above.
    private static final ThreadLocal<DecimalFormat> SCIENTIFIC_FORMAT = ThreadLocal.withInitial(() -> {
        // Scientific notation is intentionally fixed.
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        symbols.setExponentSeparator("e");

        StringBuilder pattern = new StringBuilder(4 + scientificDecimalPlaces);
        pattern.append("0.");

        // Repeat not available for tests. Don't change!
        for (int i = 0; i < scientificDecimalPlaces; i++) {
            pattern.append('#');
        }

        pattern.append("E0");

        DecimalFormat df = new DecimalFormat(pattern.toString(), symbols);
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setGroupingUsed(false);

        return df;
    });

    private NumberFormatUtil() {}

    /**
     * Certain characters are normalised due to Minecraft font rendering limitations.
     */
    private static String centralFormatter(String preFormat) {
        String postFormat = preFormat;
        postFormat = postFormat.replace("\u202F", " "); // narrow NBSP (french)
        postFormat = postFormat.replace("\u00A0", " "); // NBSP (russian)
        postFormat = postFormat.replace("\u2019", "'"); // typographic apostrophe (swiss)
        return postFormat;
    }

    /*
     * ========================= Normal formatting =========================
     */

    // These should, as much as possible, be called from the client side. If you require chat formatting, see
    // ChatComponentNumber and ChatComponentFluid.

    // Defaults
    public static String formatNumber(long number) {
        return formatNumber(number, true);
    }

    public static String formatNumber(double number) {
        return formatNumber(number, true);
    }

    public static String formatNumber(BigInteger number) {
        return formatNumber(number, true);
    }

    // Actual logic.
    public static String formatNumber(long number, boolean allowScientificNotation) {
        if (disableFormattedNotation) return String.valueOf(number);
        if (allowScientificNotation && Math.abs(number) >= scientificThreshold) return formatScientific(number);
        return centralFormatter(FORMAT.get().format(number));
    }

    public static String formatNumber(double number, boolean allowScientificNotation) {
        if (disableFormattedNotation) return String.valueOf(number);
        if (Double.isNaN(number)) return "NaN";
        if (Double.isInfinite(number)) {
            return number > 0 ? "Infinity" : "-Infinity";
        }
        if (allowScientificNotation && Math.abs(number) >= scientificThreshold) return formatScientific(number);
        return centralFormatter(FORMAT.get().format(number));
    }

    public static String formatNumber(BigInteger number, boolean allowScientificNotation) {
        if (disableFormattedNotation) return String.valueOf(number);
        if (allowScientificNotation && number.abs().compareTo(scientificThresholdBigInt) >= 0)
            return formatScientific(number);
        return centralFormatter(FORMAT.get().format(number));
    }

    /*
     * ========================= Scientific formatting =========================
     */

    private static String formatScientific(long number) {
        return SCIENTIFIC_FORMAT.get().format(number);
    }

    private static String formatScientific(double number) {
        return SCIENTIFIC_FORMAT.get().format(number);
    }

    private static String formatScientific(BigInteger number) {
        return SCIENTIFIC_FORMAT.get().format(number);
    }

    /*
     * ========================= Fluid formatting =========================
     */

    // Avoid and prefer formatFluid where possible.
    public static String getFluidUnit() {
        return NumberFormatConfig.useForgeFluidMillibuckets ? "mB" : "L";
    }

    public static String formatFluid(FluidStack fluid) {
        return formatNumber(fluid.amount) + " " + getFluidUnit();
    }

    public static String formatFluid(BigInteger number) {
        return formatNumber(number) + " " + getFluidUnit();
    }

    public static String formatFluid(long number) {
        return formatNumber(number) + " " + getFluidUnit();
    }

    public static String formatFluid(double number) {
        return formatNumber(number) + " " + getFluidUnit();
    }

    /*
     * ========================= INTERNALS ONLY =========================
     */
    public static void postConfiguration() {
        scientificThresholdBigInt = BigDecimal.valueOf(scientificThreshold).setScale(0, RoundingMode.HALF_UP)
                .toBigIntegerExact();

        resetForTests();
    }

    @VisibleForTesting
    public static void resetForTests() {
        FORMAT.remove();
        SCIENTIFIC_FORMAT.remove();
    }
}
