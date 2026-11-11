package com.gtnewhorizon.gtnhlib.color;

import java.util.stream.Stream;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class TooltipThemingGuidelines extends TextThemingGuidelineContainer {

    /** The default formatting for tooltip description text. */
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String $default = EnumChatFormatting.GRAY.toString();

    public @NotNull String getDefault() {
        return this.$default;
    }

    /** The formatting applied to the unit of certain measurements like EU/t. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String unit = EnumChatFormatting.WHITE.toString();

    /**
     * The formatting applied to the names of major components like tired multiblock components, tool names, and other
     * proper names that refer to something worth highlighting.
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String name = EnumChatFormatting.WHITE.toString();

    /**
     * The formatting applied to the names and amounts of things that are generally related to steam.<br>
     * Usually used along the hot and cold formats.
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String steam = EnumChatFormatting.WHITE.toString();

    /**
     * The formatting applied to the equation part of a formula. The term being defined is not included in this eg:<br>
     * example usage: {color:name}{color_formula: = ax+b}
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String formula = EnumChatFormatting.WHITE.toString();

    /** The formatting applied to warning. Also used to highlight an explained parameter in formulas. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String note = EnumChatFormatting.YELLOW.toString();

    /** The formatting applied to machine type descriptions. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String machineType = EnumChatFormatting.YELLOW.toString();

    /** The formatting applied to the names of miscellaneous components or tag lines. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String misc = EnumChatFormatting.BLUE.toString();

    /**
     * The formatting applied to the names and amounts of things that are generally cold.<br>
     * Usually used along the hot and steam formats.
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String cold = EnumChatFormatting.BLUE.toString();

    /** The formatting applied to processing speed modifiers. For numeric values, it is applied to "x" and "%". */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String speed = EnumChatFormatting.GREEN.toString();

    /** The formatting applied to numeric values representing a machine's effective range. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String range = EnumChatFormatting.GREEN.toString();

    /** The formatting applied to numeric values representing amounts of something. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String amount = EnumChatFormatting.GREEN.toString();

    /** The formatting applied to hatch usage notices for multiblock machines. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String hatchNotice = EnumChatFormatting.GREEN.toString();

    /** The formatting applied to efficiency loss modifiers. For numeric values, it is applied to "x" and "%". */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String loss = EnumChatFormatting.GREEN.toString();

    /** The formatting applied to values representing the number of parallel recipes a machine can handle. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String parallel = EnumChatFormatting.GOLD.toString();

    /** The formatting applied to machine mode names. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String mode = EnumChatFormatting.GOLD.toString();

    /** The formatting applied to the name of components that are key to the function of what is being described. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String key = EnumChatFormatting.GOLD.toString();

    /** The formatting applied to power efficiency modifiers. For numeric values, it is applied to "x" and "%". */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String energyEfficiency = EnumChatFormatting.AQUA.toString();

    /** The formatting applied to heat measurements. Applied to the unit. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String heat = EnumChatFormatting.RED.toString();

    /** The formatting applied to things that are generally hot. Usually used along the cold and steam formats. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String hot = EnumChatFormatting.RED.toString();

    /** The formatting applied to electron-volt measurements. Always applied to the unit. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String electronVolt = EnumChatFormatting.RED.toString();

    /** The formatting applied to depreciation notices. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String deprecated = EnumChatFormatting.RED.toString();

    /** The formatting applied to notices of dangerous aspects of something. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String danger = EnumChatFormatting.DARK_RED.toString();

    /** The formatting applied to the name of a mechanic or a mechanical component of something. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String mechanic = EnumChatFormatting.LIGHT_PURPLE.toString();

    /**
     * The formatting applied to the numeric values of time measurements. Applied to the unit when using compact
     * notation like 5s.
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String time = EnumChatFormatting.LIGHT_PURPLE.toString();

    /** The formatting applied to the numeric values of pollution measurements. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String pollution = EnumChatFormatting.DARK_PURPLE.toString();

    /** The formatting applied to the value component of power throughput measurements (eg: EU/t measurements). */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String voltage = EnumChatFormatting.YELLOW.toString();

    /**
     * Formatting applied to the value component of power throughput measurements (eg: EU/t or RF/t measurements).
     */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String amperage = EnumChatFormatting.AQUA.toString();

    /** The formatting applied to the numeric component of power capacity measurements. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String energyCapacity = EnumChatFormatting.BLUE.toString();

    /** The formatting applied to numeric component of cable loss measurements. */
    @Getter
    @Setter(AccessLevel.PRIVATE)
    private @NotNull String cableLoss = EnumChatFormatting.RED.toString();

    /** The standard themed formatter for EU voltage/throughput measurements */
    public final NumberFormatUtil.ThroughputUnitFormatter FORMAT_EU_VOLTAGE = new TextFormatUtil.ThroughputUnitFormatterWithDualTheme(
            this::getVoltage,
            this::getUnit,
            NumberFormatUtil.FORMAT_EU_PER_TICK);

    /** The standard themed formatter for EU capacity measurements */
    public final NumberFormatUtil.UnitFormatter FORMAT_EU_CAPACITY = new TextFormatUtil.UnitFormatterWithTheme(
            this::getAmperage,
            NumberFormatUtil.FORMAT_EU);

    /** The standard themed formatter for amp measurements */
    public final NumberFormatUtil.UnitFormatter FORMAT_AMP = new TextFormatUtil.UnitFormatterWithTheme(
            this::getAmperage,
            NumberFormatUtil.FORMAT_AMP);

    /** The standard themed formatter for heat measurements */
    public final NumberFormatUtil.UnitFormatter FORMAT_HEAT = new TextFormatUtil.UnitFormatterWithTheme(
            this::getHeat,
            NumberFormatUtil.FORMAT_HEAT);

    /** The standard themed formatter for electron volt measurement. */
    public final NumberFormatUtil.UnitFormatter FORMAT_ELECTRON_VOLT = new TextFormatUtil.UnitFormatterWithTheme(
            this::getElectronVolt,
            NumberFormatUtil.FORMAT_ELECTRON_VOLT);

    /** The standard themed formatter for kilo-electron volt measurement. */
    public final NumberFormatUtil.UnitFormatter FORMAT_KILO_ELECTRON_VOLT = new TextFormatUtil.UnitFormatterWithTheme(
            this::getElectronVolt,
            NumberFormatUtil.FORMAT_KEV);

    /** The standard themed formatter for mega-electron volt measurement. */
    public final NumberFormatUtil.UnitFormatter FORMAT_MEGA_ELECTRON_VOLT = new TextFormatUtil.UnitFormatterWithTheme(
            this::getElectronVolt,
            NumberFormatUtil.FORMAT_MEV);

    /** The standard themed formatter for giga-electron volt measurement. */
    public final NumberFormatUtil.UnitFormatter FORMAT_GIGA_ELECTRON_VOLT = new TextFormatUtil.UnitFormatterWithTheme(
            this::getElectronVolt,
            NumberFormatUtil.FORMAT_GEV);

    /** The standard themed formatter for tera-electron volt measurement. */
    public final NumberFormatUtil.UnitFormatter FORMAT_TERA_ELECTRON_VOLT = new TextFormatUtil.UnitFormatterWithTheme(
            this::getElectronVolt,
            NumberFormatUtil.FORMAT_TEV);

    public TooltipThemingGuidelines(@NotNull ResourceLocation @NotNull... resources) {
        super(
                Stream.concat(
                        Stream.of(new ResourceLocation(GTNHLib.MODID, "tooltip_descriptions.cfg")),
                        Stream.of(resources)).toArray(ResourceLocation[]::new));
        this.registerKey("default", this::getDefault, this::set$default);
        this.registerKey("unit", this::getUnit, this::setUnit);
        this.registerKey("name", this::getName, this::setName);
        this.registerKey("steam", this::getSteam, this::setSteam);
        this.registerKey("formula", this::getFormula, this::setFormula);
        this.registerKey("note", this::getNote, this::setNote);
        this.registerKey("machine_type", this::getMachineType, this::setMachineType);
        this.registerKey("voltage", this::getVoltage, this::setVoltage);
        this.registerKey("misc", this::getMisc, this::setMisc);
        this.registerKey("cold", this::getCold, this::setCold);
        this.registerKey("energy_capacity", this::getEnergyCapacity, this::setEnergyCapacity);
        this.registerKey("speed", this::getSpeed, this::setSpeed);
        this.registerKey("range", this::getRange, this::setRange);
        this.registerKey("amount", this::getAmount, this::setAmount);
        this.registerKey("hatch_notice", this::getHatchNotice, this::setHatchNotice);
        this.registerKey("loss", this::getLoss, this::setLoss);
        this.registerKey("parallel", this::getParallel, this::setParallel);
        this.registerKey("mode", this::getMode, this::setMode);
        this.registerKey("key", this::getKey, this::setKey);
        this.registerKey("energy_efficiency", this::getEnergyEfficiency, this::setEnergyEfficiency);
        this.registerKey("amperage", this::getAmperage, this::setAmperage);
        this.registerKey("heat", this::getHeat, this::setHeat);
        this.registerKey("hot", this::getHot, this::setHot);
        this.registerKey("electron_volt", this::getElectronVolt, this::setElectronVolt);
        this.registerKey("deprecated", this::getDeprecated, this::setDeprecated);
        this.registerKey("cable_loss", this::getCableLoss, this::setCableLoss);
        this.registerKey("danger", this::getDanger, this::setDanger);
        this.registerKey("mechanic", this::getMechanic, this::setMechanic);
        this.registerKey("time", this::getTime, this::setTime);
        this.registerKey("pollution", this::getPollution, this::setPollution);
    }
}
