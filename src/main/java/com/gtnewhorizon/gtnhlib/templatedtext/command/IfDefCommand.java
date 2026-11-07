package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Only appends the given text if a given variable exists.
///
/// Usage Example:
/// ```text
/// {if-def:minGlasstier:Needs {TC:minGlassTier:{var:minGlassTier} Glass}}
/// ```
public class IfDefCommand extends TemplatedTextCommand {

    final boolean wantsExist;

    public IfDefCommand(boolean wantsExist) {
        super(wantsExist ? "IFDEF" : "IF-NOT-DEF");
        this.wantsExist = wantsExist;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {
        String[] parts = arg.split(TemplatedTextLoader.ARG_SPLITTER_PATTERN, 2);

        // must have 2 parts
        if (parts.length != 2) {
            appendArgCountError(lineOutput, 2, parts.length);
            return;
        }

        // check if the arg is the correct type
        if (vars.containsKey(parts[0]) == wantsExist) {
            loader.emit(parts[1], lineOutput, vars);
        }
    }
}
