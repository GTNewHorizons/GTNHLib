package com.gtnewhorizon.gtnhlib.templatedtext.command;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

public abstract class TemplatedTextCommand implements ITemplatedTextCommand {

    private final String name;

    public TemplatedTextCommand(@NotNull String name) {
        this.name = name;
    }

    protected void appendArgCountError(@NotNull StringBuilder lineOutput, int needs, int has) {
        appendError(lineOutput, String.format("NEEDS %d ARGS HAS %d", needs, has));
    }

    protected void appendUnknownVarError(@NotNull StringBuilder lineOutput, @NotNull String varName) {
        appendError(lineOutput, "UNKNOWN VAR: " + varName);
    }

    protected void appendNotAVoltageIndexError(@NotNull StringBuilder lineOutput, @NotNull String argName,
            int voltageIndex) {
        String formatted = String.format("THE VALUE OF %s (%d) IS NOT A VOLTAGE TIER", argName, voltageIndex);
        appendError(lineOutput, formatted);
    }

    protected void appendError(@NotNull StringBuilder lineOutput, @NotNull String message) {
        TemplatedTextLoader.appendError(lineOutput, name, message);
    }
}
