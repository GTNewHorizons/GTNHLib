package com.gtnewhorizon.gtnhlib.templatedtext;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;
import com.gtnewhorizon.gtnhlib.templatedtext.command.DynamicFormatCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.UnitCommand;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

import java.util.function.Supplier;

public class TooltipTemplatedTextLoader extends TemplatedTextLoader {

    public TooltipTemplatedTextLoader(@NotNull String style) {
        super(style);

        // load in all the tooltip guidelines into the tooltip renderer
        TextThemingGuidelines.TOOLTIPS.getEntries()
                .forEach(entry -> this.addCommand(new DynamicFormatCommand(entry.value()), "color_" + entry.key()));

        // TODO: remove before 2.10 stable
        // spotless:off
        // backwards compatibility with some gt5u pre move
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getUnit), "color_tier");
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getHatchNotice), "color_hatch");
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getEnergyEfficiency), "color_efficiency");
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getVoltage), "color_eu_usage");
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getEnergyCapacity), "color_eu_capacity");
        // spotless:on

        this.addCommand(new UnitCommand(new ThemedEUThroughputFormatter()), "fmt-eu-voltage");
        this.addCommand(new UnitCommand(new ThemedEUCapacityFormatter()), "fmt-eu-capacity");
        this.addCommand(new UnitCommand(new ThemedAmpFormatter()), "fmt-amps");
        this.addCommand(new UnitCommand(new ThemedHeatFormatter()), "fmt-heat");
        // spotless:off
        this.addCommand(new UnitCommand(new ThemedElectronVoltFormatter(NumberFormatUtil.LOC_CACHE::getUnitElectronVolt)), "fmt-electron-volt-unit");
        this.addCommand(new UnitCommand(new ThemedElectronVoltFormatter(NumberFormatUtil.LOC_CACHE::getUnitKiloElectronVolt)), "fmt-KeV", "fmt-kev");
        this.addCommand(new UnitCommand(new ThemedElectronVoltFormatter(NumberFormatUtil.LOC_CACHE::getUnitMegaElectronVolt)), "fmt-MeV", "fmt-mev");
        this.addCommand(new UnitCommand(new ThemedElectronVoltFormatter(NumberFormatUtil.LOC_CACHE::getUnitGigaElectronVolt)), "fmt-GeV", "fmt-gev");
        this.addCommand(new UnitCommand(new ThemedElectronVoltFormatter(NumberFormatUtil.LOC_CACHE::getUnitTeraElectronVolt)), "fmt-TeV", "fmt-tev");
        // spotless:on
    }

    private static class ThemedEUThroughputFormatter extends TextFormatUtil.ThroughputUnitFormatterWithDualTheme {

        public ThemedEUThroughputFormatter() {
            super(
                    TextThemingGuidelines.TOOLTIPS::getVoltage,
                    NumberFormatUtil.LOC_CACHE::getSpacingEnergyThroughput,
                    TextThemingGuidelines.TOOLTIPS::getUnit,
                    NumberFormatUtil.LOC_CACHE::getUnitFormatEnergyThroughput,
                    NumberFormatUtil::getEnergyUnit,
                    NumberFormatUtil.LOC_CACHE::getUnitTick);
        }
    }

    private static class ThemedEUCapacityFormatter extends TextFormatUtil.UnitFormatterWithUnitTheme {

        public ThemedEUCapacityFormatter() {
            super(
                    TextThemingGuidelines.TOOLTIPS::getEnergyCapacity,
                    NumberFormatUtil.LOC_CACHE::getSpacingEnergy,
                    NumberFormatUtil::getEnergyUnit);
        }
    }

    private static class ThemedAmpFormatter extends TextFormatUtil.UnitFormatterWithTheme {

        public ThemedAmpFormatter() {
            super(
                    TextThemingGuidelines.TOOLTIPS::getAmperage,
                    NumberFormatUtil.LOC_CACHE::getSpacingEnergy,
                    NumberFormatUtil::getEnergyUnit);
        }
    }

    private static class ThemedHeatFormatter extends TextFormatUtil.UnitFormatterWithTheme {

        public ThemedHeatFormatter() {
            super(
                    TextThemingGuidelines.TOOLTIPS::getHeat,
                    NumberFormatUtil.LOC_CACHE::getSpacingHeat,
                    NumberFormatUtil::getEnergyUnit);
        }
    }

    private static class ThemedElectronVoltFormatter extends TextFormatUtil.UnitFormatterWithTheme {

        public ThemedElectronVoltFormatter(@NotNull Supplier<@NotNull String> unitSupplier) {
            super(
                TextThemingGuidelines.TOOLTIPS::getElectronVolt,
                NumberFormatUtil.LOC_CACHE::getSpacingElectronVolt,
                unitSupplier);
        }
    }

    @Override
    protected String formatLine(String parsedLine) {
        return TextThemingGuidelines.TOOLTIPS.getDefault() + parsedLine;
    }
}
