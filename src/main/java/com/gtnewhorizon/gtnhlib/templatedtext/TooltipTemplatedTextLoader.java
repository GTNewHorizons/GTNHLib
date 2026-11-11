package com.gtnewhorizon.gtnhlib.templatedtext;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;
import com.gtnewhorizon.gtnhlib.templatedtext.command.DynamicFormatCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.command.UnitCommand;

public class TooltipTemplatedTextLoader extends TemplatedTextLoader {

    public TooltipTemplatedTextLoader(@NotNull String style) {
        super(style);

        // load in all the tooltip guidelines into the tooltip renderer
        TextThemingGuidelines.TOOLTIPS.getEntries()
                .forEach(entry -> this.addCommand(new DynamicFormatCommand(entry.value()), "color_" + entry.key()));

        // spotless:off
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_EU_VOLTAGE), "fmt-eu-voltage");
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_EU_CAPACITY), "fmt-eu-capacity");
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_AMP), "fmt-amps");
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_HEAT), "fmt-heat");
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_ELECTRON_VOLT), "fmt-electron-volt-unit");
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_KILO_ELECTRON_VOLT), "fmt-KeV", "fmt-kev");
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_MEGA_ELECTRON_VOLT), "fmt-MeV", "fmt-mev");
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_GIGA_ELECTRON_VOLT), "fmt-GeV", "fmt-gev");
        this.addCommand(new UnitCommand(TextThemingGuidelines.TOOLTIPS.FORMAT_TERA_ELECTRON_VOLT), "fmt-TeV", "fmt-tev");

        // TODO: remove before 2.10 stable
        // backwards compatibility with some gt5u pre move
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getUnit), "color_tier");
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getHatchNotice), "color_hatch");
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getEnergyEfficiency), "color_efficiency");
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getVoltage), "color_eu_usage");
        this.addCommand(new DynamicFormatCommand(TextThemingGuidelines.TOOLTIPS::getEnergyCapacity), "color_eu_capacity");
        // spotless:on

    }

    @Override
    protected String formatLine(String parsedLine) {
        return TextThemingGuidelines.TOOLTIPS.getDefault() + parsedLine;
    }
}
