package com.gtnewhorizon.gtnhlib.templatedtext;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.color.TextThemingGuidelines;
import com.gtnewhorizon.gtnhlib.templatedtext.command.DynamicFormatCommand;

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
    }

    @Override
    protected String formatLine(String parsedLine) {
        return TextThemingGuidelines.TOOLTIPS.getDefault() + parsedLine;
    }
}
