package com.gtnewhorizon.gtnhlib.templatedtext.command;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Allows the insertion of plurality suffixes depending on a numeric variable.
///
/// Usage Example
/// ```text
/// There {plural:count:are}{singular:count:is} {var:count} bird{plural:count:s}
/// ```
public class PluralityCommand extends TemplatedTextCommand {

    final boolean wantsSingular;

    public PluralityCommand(boolean wantsSingular) {
        super(wantsSingular ? "SINGULAR" : "PLURAL");
        this.wantsSingular = wantsSingular;
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
        Object unparsed = context.getVar(parts[0]);
        boolean result;
        if (unparsed instanceof String v) {
            result = wantsSingular ^ (!v.equals("1") && !v.equals("-1"));
        } else if (unparsed instanceof Number v) {
            result = wantsSingular ^ (v.longValue() != 1L && v.longValue() != -1L);
        } else {
            appendUnknownVarError(lineOutput, arg);
            return;
        }

        // check if it's a valid value
        if (result) loader.emit(parts[1], lineOutput, context);
    }
}
