package com.gtnewhorizon.gtnhlib.templatedtext.command;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Only appends the given text if the variable parameter has the right boolean value.
///
/// Usage Examples:
/// ```text
/// {if-true:boolVar:Shown if boolVar is true}
/// {if-false:boolVar:Shown if boolVar is false}
/// ```
public class IfCommand extends TemplatedTextCommand {

    final boolean wantsTrue;

    public IfCommand(boolean mustBeTrue) {
        super(mustBeTrue ? "IF-TRUE" : "IF-FALSE");
        this.wantsTrue = mustBeTrue;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {
        String[] parts = TemplatedTextLoader.splitArgs(arg, 2);

        // must have 2 parts
        if (parts.length != 2) {
            appendArgCountError(lineOutput, 2, parts.length);
            return;
        }

        // check if the arg is the correct type
        if (!(context.getVar(parts[0]) instanceof Boolean value)) {
            appendUnknownVarError(lineOutput, arg);
            return;
        }

        // check if it's a valid value
        if (value == this.wantsTrue) loader.emit(parts[1], lineOutput, context);
    }
}
