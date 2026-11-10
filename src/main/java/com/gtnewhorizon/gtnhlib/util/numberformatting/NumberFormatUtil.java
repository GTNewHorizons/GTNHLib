package com.gtnewhorizon.gtnhlib.util.numberformatting;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_BILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_MILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_QUADRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_THOUSAND;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.Constants.BD_TRILLION;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig.*;
import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.CompactOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.util.internal.ConcurrentSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;

@SuppressWarnings("unused")
public final class NumberFormatUtil {

    /* ========================= Formatters ========================= */

    private NumberFormatUtil() {}

    /* ========================= Localisation ========================= */

    public static class LocalisationCache {

        @Target({ FIELD })
        @Retention(RetentionPolicy.RUNTIME)
        private @interface LangKey {

            @NotNull
            String value();
        }

        // numeric abbreviation

        @LangKey("gtnhlib.unified.abbreviation.format")
        @Getter
        private @NotNull String abbreviationFormat = "%s%s";

        @LangKey("gtnhlib.unified.abbreviation.thousand")
        @Getter
        private @NotNull String abbreviationThousand = "K";

        @LangKey("gtnhlib.unified.abbreviation.million")
        @Getter
        private @NotNull String abbreviationMillion = "M";

        @LangKey("gtnhlib.unified.abbreviation.billion")
        @Getter
        private @NotNull String abbreviationBillion = "B";

        @LangKey("gtnhlib.unified.abbreviation.trillion")
        @Getter
        private @NotNull String abbreviationTrillion = "T";

        @LangKey("gtnhlib.unified.abbreviation.quadrillion")
        @Getter
        private @NotNull String abbreviationQuadrillion = "Q";

        @LangKey("gtnhlib.unified.abbreviation.quintillion")
        @Getter
        private @NotNull String abbreviationQuintillion = "P";

        // exponential formatting

        @LangKey("gtnhlib.unified.exponential_format.scientific")
        @Getter
        private @NotNull String exponentialFormatScientific = "%se%s";

        @LangKey("gtnhlib.unified.exponential_format.engineering")
        @Getter
        private @NotNull String exponentialFormatEngineering = "%se%s";

        @LangKey("gtnhlib.unified.exponential_format.power_of_ten")
        @Getter
        private @NotNull String exponentialFormatPowerOfTen = "%s*10^%s";

        // units

        @LangKey("gtnhlib.unified.unit.fluid.mb")
        @Getter
        private @NotNull String unitMillibucket = "mB";

        @LangKey("gtnhlib.unified.unit.fluid.liter")
        @Getter
        private @NotNull String unitLiter = "L";

        @LangKey("gtnhlib.unified.unit.RF")
        @Getter
        private @NotNull String unitRF = "RF";

        @LangKey("gtnhlib.unified.unit.MJ")
        @Getter
        private @NotNull String unitMJ = "MJ";

        @LangKey("gtnhlib.unified.unit.AE")
        @Getter
        private @NotNull String unitAE = "AE";

        @LangKey("gtnhlib.unified.unit.EU")
        @Getter()
        private @NotNull String unitEU = "EU";

        @LangKey("gtnhlib.unified.unit.amp")
        @Getter
        private @NotNull String unitAmp = "A";

        @LangKey("gtnhlib.unified.unit.heat")
        @Getter
        private @NotNull String unitHeat = "K";

        @LangKey("gtnhlib.unified.unit.acidity")
        @Getter
        private @NotNull String unitAcidity = "pH";

        @LangKey("gtnhlib.unified.unit.radiation")
        @Getter
        private @NotNull String unitRadiation = "Sv";

        @LangKey("gtnhlib.unified.unit.temperature")
        @Getter
        private @NotNull String unitTemperature = "°C";

        @LangKey("gtnhlib.unified.unit.force")
        @Getter
        private @NotNull String UnitForce = "N";

        @LangKey("gtnhlib.unified.unit.electron_volt")
        @Getter
        private @NotNull String unitElectronVolt = "eV";

        @LangKey("gtnhlib.unified.unit.kilo_electron_volt")
        @Getter
        private @NotNull String unitKiloElectronVolt = "KeV";

        @LangKey("gtnhlib.unified.unit.mega_electron_volt")
        @Getter
        private @NotNull String unitMegaElectronVolt = "MeV";

        @LangKey("gtnhlib.unified.unit.giga_electron_volt")
        @Getter
        private @NotNull String unitGigaElectronVolt = "GeV";

        @LangKey("gtnhlib.unified.unit.tera_electron_volt")
        @Getter
        private @NotNull String unitTeraElectronVolt = "TeV";

        @LangKey("gtnhlib.unified.unit.nanogram")
        @Getter
        private @NotNull String unitNanogram = "ng";

        @LangKey("gtnhlib.unified.unit.microgram")
        @Getter
        private @NotNull String unitMicrogram = "μg";

        @LangKey("gtnhlib.unified.unit.milligram")
        @Getter
        private @NotNull String unitMilligram = "mg";

        @LangKey("gtnhlib.unified.unit.gram")
        @Getter
        private @NotNull String unitGram = "g";

        @LangKey("gtnhlib.unified.unit.kilogram")
        @Getter
        private @NotNull String unitKilogram = "kg";

        @LangKey("gtnhlib.unified.unit.nanometer")
        @Getter
        private @NotNull String unitNanometer = "nm";

        @LangKey("gtnhlib.unified.unit.micrometer")
        @Getter
        private @NotNull String unitMicrometer = "μm";

        @LangKey("gtnhlib.unified.unit.millimeter")
        @Getter
        private @NotNull String unitMillimeter = "mm";

        @LangKey("gtnhlib.unified.unit.centimeter")
        @Getter
        private @NotNull String unitCentimeter = "cm";

        @LangKey("gtnhlib.unified.unit.meter")
        @Getter
        private @NotNull String unitMeter = "m";

        @LangKey("gtnhlib.unified.unit.kilometer")
        @Getter
        private @NotNull String unitKilometer = "km";

        @LangKey("gtnhlib.unified.unit.nanosecond")
        @Getter
        private @NotNull String unitNanosecond = "ns";

        @LangKey("gtnhlib.unified.unit.microsecond")
        @Getter
        private @NotNull String unitMicrosecond = "μs";

        @LangKey("gtnhlib.unified.unit.millisecond")
        @Getter
        private @NotNull String unitMillisecond = "ms";

        @LangKey("gtnhlib.unified.unit.tick")
        @Getter
        private @NotNull String unitTick = "t";

        @LangKey("gtnhlib.unified.unit.second")
        @Getter
        private @NotNull String unitSecond = "s";

        @LangKey("gtnhlib.unified.unit.minute")
        @Getter
        private @NotNull String unitMinute = "m";

        @LangKey("gtnhlib.unified.unit.hour")
        @Getter
        private @NotNull String unitHour = "h";

        @LangKey("gtnhlib.unified.unit.day")
        @Getter
        private @NotNull String unitDay = "d";

        // special values

        @LangKey("gtnhlib.unified.value.infinity")
        @Getter
        private @NotNull String specialValueInfinity = "Infinity";

        @LangKey("gtnhlib.unified.value.neg_infinity")
        @Getter
        private @NotNull String specialValueNegInfinity = "-Infinity";

        @LangKey("gtnhlib.unified.value.nan")
        @Getter
        private @NotNull String specialValueNaN = "NaN";

        @LangKey("gtnhlib.unified.value.null")
        @Getter
        private @NotNull String specialValueNull = "NULL";

        // unit formatting

        @LangKey("gtnhlib.unified.measurement.spacing.fluid")
        @Getter
        private @NotNull String spacingFluid = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.energy")
        @Getter
        private @NotNull String spacingEnergy = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.amperage")
        @Getter
        private @NotNull String spacingAmperage = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.heat")
        @Getter
        private @NotNull String spacingHeat = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.electron_volt")
        @Getter
        private @NotNull String spacingElectronVolt = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.acidity")
        @Getter
        private @NotNull String spacingAcidity = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.radiation")
        @Getter
        private @NotNull String spacingRadiation = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.temperature")
        @Getter
        private @NotNull String spacingTemperature = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.force")
        @Getter
        private @NotNull String spacingForce = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.mass")
        @Getter
        private @NotNull String spacingMass = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.distance")
        @Getter
        private @NotNull String spacingDistance = "%s %s";

        @LangKey("gtnhlib.unified.measurement.spacing.time")
        @Getter
        private @NotNull String spacingTime = "%s %s";

        // thoughput unit formatting

        @LangKey("gtnhlib.unified.measurement.spacing.fluid_throughput")
        @Getter
        private @NotNull String spacingFluidThroughput = "%s %s";

        @LangKey("gtnhlib.unified.measurement.unit_format.fluid_throughput")
        @Getter
        private @NotNull String unitFormatFluidThroughput = "%s/%s";

        @LangKey("gtnhlib.unified.measurement.spacing.energy_throughput")
        @Getter
        private @NotNull String spacingEnergyThroughput = "%s %s";

        @LangKey("gtnhlib.unified.measurement.unit_format.energy_throughput")
        @Getter
        private @NotNull String unitFormatEnergyThroughput = "%s/%s";

        @LangKey("gtnhlib.unified.measurement.spacing.mass_throughput")
        @Getter
        private @NotNull String spacingMassThroughput = "%s %s";

        @LangKey("gtnhlib.unified.measurement.unit_format.mass_throughput")
        @Getter
        private @NotNull String unitFormatMassThroughput = "%s/%s";

        @LangKey("gtnhlib.unified.measurement.spacing.speed")
        @Getter
        private @NotNull String spacingSpeed = "%s %s";

        @LangKey("gtnhlib.unified.measurement.unit_format.speed")
        @Getter
        private @NotNull String unitFormatSpeed = "%s/%s";

        private final @NotNull Object2ObjectOpenHashMap<@NotNull String, @NotNull String> defaults = new Object2ObjectOpenHashMap<>();

        @SideOnly(Side.CLIENT)
        private synchronized void onResourceManagerReload(IResourceManager resourceManager) {
            boolean initDefaults = defaults.isEmpty();
            try {
                // update the field values
                for (final Field field : this.getClass().getDeclaredFields()) {
                    // we only care about string fields with the lang key annotation
                    if (field.getType() != String.class) continue;
                    LangKey annotation = field.getAnnotation(LangKey.class);
                    if (annotation == null) continue;
                    String langKey = annotation.value();

                    // init defaults if needed
                    if (initDefaults) {
                        defaults.computeIfAbsent(annotation.value(), ($_) -> {
                            try {
                                return (String) FieldUtils.readField(field, this, true);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }

                    // attempt to translate, else fallback to the default value, and then they key itself.
                    String translated;
                    if (!StatCollector.canTranslate(langKey)) {
                        translated = this.defaults.getOrDefault(annotation.value(), langKey);
                    } else {
                        translated = StatCollector.translateToLocal(annotation.value());
                    }

                    // update the field value
                    FieldUtils.writeField(field, this, translated, true);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ReloadListener implements IResourceManagerReloadListener {

        private static final ReloadListener INSTANCE = new ReloadListener();

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
            LOC_CACHE.onResourceManagerReload(resourceManager);
            // run the callbacks that pre-cache formatting to an even higher degree.
            recacheUnitFormatters();
        }
    }

    private static final ConcurrentSet<ThroughputUnitFormatter> THROUGHPUT_UNIT_FORMATTERS = new ConcurrentSet<>();

    @ApiStatus.Internal
    public static void recacheUnitFormatters() {
        THROUGHPUT_UNIT_FORMATTERS.forEach(ThroughputUnitFormatter::updateCachedUnitString);
    }

    @SideOnly(Side.CLIENT)
    @ApiStatus.Internal
    public static IResourceManagerReloadListener getReloadListener() {
        return ReloadListener.INSTANCE;
    }

    public final static LocalisationCache LOC_CACHE = new LocalisationCache();

    /* ========================= Main Formatter ========================= */

    public static String formatNumber(Number value) {
        return formatNumber(value, new FormatOptions());
    }

    public static String formatNumber(Number value, FormatOptions options) {
        String special = handleSpecialCases(value);
        if (special != null) return special;

        BigDecimal val = bigDecimalConverter(value);
        BigDecimal abs = val.abs();

        // Exponential for more than 1T by default
        if (isExponentialFormattingEnabled(abs, options)) {
            return formatExponential(val, options);
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
     * (K/M/B), greater than 1T becomes exponential. Options objects can modify this behaviour.
     */
    public static String formatNumberCompact(Number value, CompactOptions options) {
        String special = handleSpecialCases(value);
        if (special != null) return special;

        BigDecimal val = bigDecimalConverter(value);
        BigDecimal abs = val.abs();

        // Below threshold, do exact formatting
        if (abs.compareTo(options.getCompactThreshold()) < 0) {
            return formatNumber(value, options.toFormatOptions());
        }

        // More than 1T, do exponential.
        if (isExponentialFormattingEnabled(abs, options)) {
            return formatExponential(val, options);
        }

        // Abbreviated (1K, 2.5M etc)
        String ret = abbreviate(val, options);
        // fallback in case something goes really wrong
        if (ret == null) return formatNumber(value, options.toFormatOptions());
        return ret;
    }

    /* ========================= Abbreviation ========================= */

    private static final class CompactDividerThreshold {

        public final BigDecimal number;
        public final Supplier<String> supplier;

        private CompactDividerThreshold(BigDecimal number, Supplier<String> supplier) {
            this.number = number;
            this.supplier = supplier;
        }
    }

    public static class AbbreviatedNumberInfo {

        public final BigDecimal rounded;
        public final int exp;
        public final String abbvr;

        public AbbreviatedNumberInfo(BigDecimal rounded, int exp, String abbvr) {
            this.rounded = rounded;
            this.exp = exp;
            this.abbvr = abbvr;
        }
    }

    private static final ImmutableList<CompactDividerThreshold> COMPACT_DIVIDERS = ImmutableList
            .<CompactDividerThreshold>builder()
            .add(new CompactDividerThreshold(BD_QUADRILLION, LOC_CACHE::getAbbreviationQuadrillion))
            .add(new CompactDividerThreshold(BD_TRILLION, LOC_CACHE::getAbbreviationTrillion))
            .add(new CompactDividerThreshold(BD_BILLION, LOC_CACHE::getAbbreviationBillion))
            .add(new CompactDividerThreshold(BD_MILLION, LOC_CACHE::getAbbreviationMillion))
            .add(new CompactDividerThreshold(BD_THOUSAND, LOC_CACHE::getAbbreviationThousand)).build();

    /**
     * @return null if the compact divider list was somehow empty (probably due to a loc loading issue)
     */
    public static @Nullable AbbreviatedNumberInfo getAbbreviatedNumberInformation(BigDecimal number,
            NumberOptionsBase<?> options) {
        // localing to prevent race conditions, because O plan on making this a localizable list down the line for
        // languages like japanese that use compact number thresholds.
        final ImmutableList<CompactDividerThreshold> compactDividers = COMPACT_DIVIDERS;
        if (compactDividers.isEmpty()) return null;

        // find the divider that matches this
        final BigDecimal abs = number.abs();
        int dividerIdx = 0;
        for (; dividerIdx < compactDividers.size() - 1; dividerIdx++) {
            if (compactDividers.get(dividerIdx).number.compareTo(abs) <= 0) break;
        }
        CompactDividerThreshold threshold = compactDividers.get(dividerIdx);

        // Prevent unit rollover caused only by rounding:
        // If abs(value) is still below the next threshold, do not allow rounded to reach 1000.
        BigDecimal scaled = number.divide(threshold.number, MathContext.UNLIMITED);
        int dp = options.getMaxDecimalPlaces();
        RoundingMode rm = options.getRoundingMode();

        BigDecimal rounded = scaled.setScale(dp, rm);

        BigDecimal nextThreshold = threshold.number.multiply(BD_THOUSAND);
        if (abs.compareTo(nextThreshold) < 0 && rounded.abs().compareTo(BD_THOUSAND) >= 0) {
            BigDecimal step = BigDecimal.ONE.scaleByPowerOfTen(-dp); // 10^-dp (works for dp <= 0 too)
            BigDecimal maxAbs = BD_THOUSAND.subtract(step).setScale(dp, RoundingMode.UNNECESSARY);
            rounded = (rounded.signum() < 0) ? maxAbs.negate() : maxAbs;
        }

        rounded = rounded.stripTrailingZeros();

        return new AbbreviatedNumberInfo(
                rounded,
                threshold.number.precision() - 1 - threshold.number.scale(),
                threshold.supplier.get());
    }

    private static @Nullable String abbreviate(BigDecimal value, NumberOptionsBase<?> options) {

        AbbreviatedNumberInfo info = getAbbreviatedNumberInformation(value, options);
        if (info == null) return null;

        return String.format(
                LOC_CACHE.abbreviationFormat,
                formatNumber(info.rounded, options.toFormatOptions()),
                info.abbvr);
    }

    /* ========================= Exponential ========================= */

    private static String formatExponential(BigDecimal value, NumberOptionsBase<?> options) {
        return options.getExponentialFormat().format(value, options);
    }

    /* ========================= Fluids ========================= */

    public static @NotNull String getFluidUnit() {
        if (debugFluidUnit) return "X";
        return StatCollector
                .translateToLocal(useForgeFluidMillibuckets ? LOC_CACHE.unitMillibucket : LOC_CACHE.unitLiter);
    }

    public static String formatFluid(Number value) {
        return formatFluid(value, new FormatOptions());
    }

    public static String formatFluid(Number value, FormatOptions options) {
        return String.format(LOC_CACHE.spacingFluid, formatNumber(value, options), getFluidUnit());
    }

    public static String formatFluidCompact(Number value) {
        return formatFluidCompact(value, new CompactOptions());
    }

    public static String formatFluidCompact(Number value, CompactOptions options) {
        return String.format(LOC_CACHE.spacingFluid, formatNumberCompact(value, options), getFluidUnit());
    }

    public static String formatFluid(FluidStack stack) {
        return formatFluid(stack.amount);
    }

    /* ========================= Energy ========================= */

    public static @NotNull String getEnergyUnit() {
        return LOC_CACHE.unitEU;
    }

    public static String formatEnergy(Number value) {
        return formatEnergy(value, new FormatOptions());
    }

    public static String formatEnergy(Number value, FormatOptions options) {
        return String.format(LOC_CACHE.spacingEnergy, formatNumber(value, options), getEnergyUnit());
    }

    public static String formatEnergyCompact(Number value, CompactOptions options) {
        return String.format(LOC_CACHE.spacingEnergy, formatNumberCompact(value, options), getEnergyUnit());
    }

    public static String formatEnergyCompact(Number value) {
        return formatEnergyCompact(value, new CompactOptions());
    }

    /* ========================= Unit Formatters ========================= */

    public static final @NotNull IUnitFormatter FORMAT_FLUID = new UnitFormatter(
            LOC_CACHE::getSpacingFluid,
            NumberFormatUtil::getFluidUnit);

    public static final @NotNull IUnitFormatter FORMAT_RF = new UnitFormatter(
            LOC_CACHE::getSpacingEnergy,
            LOC_CACHE::getUnitRF);

    public static final @NotNull IUnitFormatter FORMAT_MJ = new UnitFormatter(
            LOC_CACHE::getSpacingEnergy,
            LOC_CACHE::getUnitMJ);

    public static final @NotNull IUnitFormatter FORMAT_AE = new UnitFormatter(
            LOC_CACHE::getSpacingEnergy,
            LOC_CACHE::getUnitAE);

    public static final @NotNull IUnitFormatter FORMAT_EU = new UnitFormatter(
            LOC_CACHE::getSpacingEnergy,
            LOC_CACHE::getUnitEU);

    public static final @NotNull IUnitFormatter FORMAT_AMP = new UnitFormatter(
            LOC_CACHE::getSpacingAmperage,
            LOC_CACHE::getUnitAmp);

    public static final @NotNull IUnitFormatter FORMAT_HEAT = new UnitFormatter(
            LOC_CACHE::getSpacingHeat,
            LOC_CACHE::getUnitHeat);

    public static final @NotNull IUnitFormatter FORMAT_ACIDITY = new UnitFormatter(
            LOC_CACHE::getSpacingAcidity,
            LOC_CACHE::getUnitAcidity);

    public static final @NotNull IUnitFormatter FORMAT_RADIATION = new UnitFormatter(
            LOC_CACHE::getSpacingRadiation,
            LOC_CACHE::getUnitRadiation);

    public static final @NotNull IUnitFormatter FORMAT_TEMPERATURE = new UnitFormatter(
            LOC_CACHE::getSpacingTemperature,
            LOC_CACHE::getUnitTemperature);

    public static final @NotNull IUnitFormatter FORMAT_FORCE = new UnitFormatter(
            LOC_CACHE::getSpacingForce,
            LOC_CACHE::getUnitForce);

    // electron volt units

    public static final @NotNull IUnitFormatter FORMAT_ELECTRON_VOLT = new UnitFormatter(
            LOC_CACHE::getSpacingElectronVolt,
            LOC_CACHE::getUnitElectronVolt);

    public static final @NotNull IUnitFormatter FORMAT_KEV = new UnitFormatter(
            LOC_CACHE::getSpacingElectronVolt,
            LOC_CACHE::getUnitKiloElectronVolt);

    public static final @NotNull IUnitFormatter FORMAT_MEV = new UnitFormatter(
            LOC_CACHE::getSpacingElectronVolt,
            LOC_CACHE::getUnitMegaElectronVolt);

    public static final @NotNull IUnitFormatter FORMAT_GEV = new UnitFormatter(
            LOC_CACHE::getSpacingElectronVolt,
            LOC_CACHE::getUnitGigaElectronVolt);

    public static final @NotNull IUnitFormatter FORMAT_TEV = new UnitFormatter(
            LOC_CACHE::getSpacingElectronVolt,
            LOC_CACHE::getUnitTeraElectronVolt);

    // mass units

    public static final @NotNull IUnitFormatter FORMAT_NANOGRAM = new UnitFormatter(
            LOC_CACHE::getSpacingMass,
            LOC_CACHE::getUnitNanogram);

    public static final @NotNull IUnitFormatter FORMAT_MICROGRAM = new UnitFormatter(
            LOC_CACHE::getSpacingMass,
            LOC_CACHE::getUnitMicrogram);

    public static final @NotNull IUnitFormatter FORMAT_MILLIGRAM = new UnitFormatter(
            LOC_CACHE::getSpacingMass,
            LOC_CACHE::getUnitMilligram);

    public static final @NotNull IUnitFormatter FORMAT_GRAM = new UnitFormatter(
            LOC_CACHE::getSpacingMass,
            LOC_CACHE::getUnitGram);

    public static final @NotNull IUnitFormatter FORMAT_KILOGRAM = new UnitFormatter(
            LOC_CACHE::getSpacingMass,
            LOC_CACHE::getUnitKilogram);

    // distance units

    public static final @NotNull IUnitFormatter FORMAT_NANOMETER = new UnitFormatter(
            LOC_CACHE::getSpacingDistance,
            LOC_CACHE::getUnitNanometer);

    public static final @NotNull IUnitFormatter FORMAT_MICROMETER = new UnitFormatter(
            LOC_CACHE::getSpacingDistance,
            LOC_CACHE::getUnitMicrometer);

    public static final @NotNull IUnitFormatter FORMAT_MILLIMETER = new UnitFormatter(
            LOC_CACHE::getSpacingDistance,
            LOC_CACHE::getUnitMillimeter);

    public static final @NotNull IUnitFormatter FORMAT_CENTIMETER = new UnitFormatter(
            LOC_CACHE::getSpacingDistance,
            LOC_CACHE::getUnitCentimeter);

    public static final @NotNull IUnitFormatter FORMAT_METER = new UnitFormatter(
            LOC_CACHE::getSpacingDistance,
            LOC_CACHE::getUnitMeter);

    public static final @NotNull IUnitFormatter FORMAT_KILOMETER = new UnitFormatter(
            LOC_CACHE::getSpacingDistance,
            LOC_CACHE::getUnitKilometer);

    // time units

    public static final @NotNull IUnitFormatter FORMAT_NANOSECOND = new UnitFormatter(
            LOC_CACHE::getSpacingTime,
            LOC_CACHE::getUnitNanosecond);

    public static final @NotNull IUnitFormatter FORMAT_MICROSECOND = new UnitFormatter(
            LOC_CACHE::getSpacingTime,
            LOC_CACHE::getUnitMicrosecond);

    public static final @NotNull IUnitFormatter FORMAT_MILLISECOND = new UnitFormatter(
            LOC_CACHE::getSpacingTime,
            LOC_CACHE::getUnitMillisecond);

    public static final @NotNull IUnitFormatter FORMAT_TICK = new UnitFormatter(
            LOC_CACHE::getSpacingTime,
            LOC_CACHE::getUnitTick);

    public static final @NotNull IUnitFormatter FORMAT_SECOND = new UnitFormatter(
            LOC_CACHE::getSpacingTime,
            LOC_CACHE::getUnitSecond);

    public static final @NotNull IUnitFormatter FORMAT_MINUTE = new UnitFormatter(
            LOC_CACHE::getSpacingTime,
            LOC_CACHE::getUnitMinute);

    public static final @NotNull IUnitFormatter FORMAT_HOUR = new UnitFormatter(
            LOC_CACHE::getSpacingTime,
            LOC_CACHE::getUnitHour);

    public static final @NotNull IUnitFormatter FORMAT_DAY = new UnitFormatter(
            LOC_CACHE::getSpacingTime,
            LOC_CACHE::getUnitDay);

    /* ========================= Internals ========================= */

    public interface IUnitFormatter {

        @NotNull
        String getUnit();

        @NotNull
        String format(@Nullable String value);

        @NotNull
        String format(@Nullable Number value, @NotNull NumberOptionsBase<?> options);

        default @NotNull String format(@Nullable Number value) {
            return this.format(value, new FormatOptions());
        }

        default @NotNull String formatCompact(@Nullable Number value) {
            return this.format(value, new CompactOptions());
        }

    }

    public static class UnitFormatter implements IUnitFormatter {

        private final @NotNull Supplier<@NotNull String> formatSupplier;
        private final @NotNull Supplier<@NotNull String> unitSupplier;

        public UnitFormatter(@NotNull Supplier<@NotNull String> formatSupplier,
                @NotNull Supplier<@NotNull String> unitSupplier) {
            this.formatSupplier = formatSupplier;
            this.unitSupplier = unitSupplier;
        }

        @Override
        public @NotNull String getUnit() {
            return unitSupplier.get();
        }

        @Override
        public @NotNull String format(@Nullable String value) {
            return String.format(this.formatSupplier.get(), value, unitSupplier.get());
        }

        @Override
        public @NotNull String format(@Nullable Number value, @NotNull NumberOptionsBase<?> options) {
            return String.format(this.formatSupplier.get(), options.format(value), unitSupplier.get());
        }

    }

    public static class ThroughputUnitFormatter implements IUnitFormatter, AutoCloseable {

        private final @NotNull Supplier<@NotNull String> formatSupplier;
        private final @NotNull Supplier<@NotNull String> unitFormatSupplier;
        private final @NotNull Supplier<@NotNull String> unit1Supplier;
        private final @NotNull Supplier<@NotNull String> unit2Supplier;
        private @NotNull String cachedUnitText;

        public ThroughputUnitFormatter(@NotNull Supplier<@NotNull String> formatSupplier,
                @NotNull Supplier<@NotNull String> unitFormatSupplier, @NotNull Supplier<@NotNull String> unit1Supplier,
                @NotNull Supplier<@NotNull String> unit2Supplier) {
            this.formatSupplier = formatSupplier;
            this.unitFormatSupplier = unitFormatSupplier;
            this.unit1Supplier = unit1Supplier;
            this.unit2Supplier = unit2Supplier;
            THROUGHPUT_UNIT_FORMATTERS.add(this);
            updateCachedUnitString();
        }

        @VisibleForTesting
        @Override
        public void close() {
            THROUGHPUT_UNIT_FORMATTERS.remove(this);
        }

        private void updateCachedUnitString() {
            this.cachedUnitText = this.createCachedUnitString();
        }

        protected String createCachedUnitString() {
            return String.format(this.unitFormatSupplier.get(), this.unit1Supplier.get(), this.unit2Supplier.get());
        }

        @Override
        public @NotNull String getUnit() {
            return cachedUnitText;
        }

        @Override
        public @NotNull String format(@Nullable String value) {
            return String.format(this.formatSupplier.get(), value, this.cachedUnitText);
        }

        @Override
        public @NotNull String format(@Nullable Number value, @NotNull NumberOptionsBase<?> options) {
            return this.format(options.format(value));
        }

    }

    public static String rawNumberToString(Number value) {
        if (value instanceof BigDecimal bd) {
            // It seems BigDecimal.toString() may emit exponential notation, but
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

    public static boolean isExponentialFormattingEnabled(BigDecimal absValue, NumberOptionsBase<?> options) {
        // Global flag disables exponential formatting entirely
        boolean disabledGlobally = globalDisableExponentialNotation;

        // Whether this specific number qualifies for exponential formatting
        boolean aboveThreshold = absValue.compareTo(options.getExponentialThreshold()) >= 0;

        // Whether exponential formatting is enabled for this option
        boolean enabledInOptions = options.isExponentialFormattingEnabled();

        // Exponential formatting is enabled only if all conditions are true
        return !disabledGlobally && enabledInOptions && aboveThreshold;
    }

    private static DecimalFormat getDefaultDecimalFormatter(NumberOptionsBase<?> options) {
        // Use custom locale from config if set, otherwise use system default
        Locale locale = NumberFormatConfig.getActiveLocale();

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
        df.setDecimalFormatSymbols(symbols);
        df.setGroupingUsed(true);
        df.setMaximumFractionDigits(options.getMaxDecimalPlaces());
        df.setMinimumFractionDigits(options.getMinDecimalPlaces());
        df.setRoundingMode(options.getRoundingMode());

        return df;
    }

    private static String handleSpecialCases(Number value) {
        if (value == null) return LOC_CACHE.specialValueNull;
        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            if (Double.isNaN(d)) return LOC_CACHE.specialValueNaN;
            if (Double.isInfinite(d)) return d > 0 ? LOC_CACHE.specialValueInfinity : LOC_CACHE.specialValueNegInfinity;
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
        NumberFormatConfig.syncNumberFormatting();
        // recache unit formatter in case some are dependent on configs
        NumberFormatUtil.recacheUnitFormatters();
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

    public static String toBinaryString(int i, int minimumWidth) {
        if (minimumWidth < 1) {
            throw new IllegalArgumentException("Width must be positive.");
        }

        String binary = Integer.toBinaryString(i);
        int currentWidth = binary.length();
        if (currentWidth < minimumWidth) {
            binary = String.format("%0" + (minimumWidth - currentWidth) + "d", 0).concat(binary);
        }

        return binary;
    }

}
