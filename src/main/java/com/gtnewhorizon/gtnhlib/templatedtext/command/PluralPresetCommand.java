package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.FormatOptions;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;
import com.gtnewhorizon.gtnhlib.util.parsing.CLDRPluralRuleNumber;
import com.gtnewhorizon.gtnhlib.util.parsing.CLDRPluralRuleParser;

/// Allows the insertion of plurality suffixes depending on a numeric variable.
///
/// Usage Examples:
/// ```text
/// {var:var_name} {plural-fr:var_name:jour:de jours:jours}
/// {var:var_name} корпус{plural-uk:var_name:день:дні:днів:дня}
/// ```
public class PluralPresetCommand extends TemplatedTextCommand {

    private final @NotNull CLDRPluralRuleParser parser;
    private final int expectedPluralCount;

    public PluralPresetCommand(@NotNull CLDRPluralRuleParser parser, @NotNull String name) {
        super("PLURAL-" + name);
        this.parser = parser;
        this.expectedPluralCount = parser.getRuleCount() + 1;
    }

    public static @Nullable CLDRPluralRuleNumber getCLDRPluralRuleNumber(@NotNull TemplatedTextContext context,
            @NotNull String varName) {
        // check if the arg is the correct type
        Object unparsed = context.getVar(varName);
        if (!(unparsed instanceof Number number)) {
            return null;
        }
        // get the format if it exists
        Object format = context.getVar(varName + "-fmt");
        if (format instanceof NumberOptionsBase<?>options) {
            return new CLDRPluralRuleNumber(number, options);
        }
        return new CLDRPluralRuleNumber(number, new FormatOptions());
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {

        // validate part count
        String[] parts = TemplatedTextLoader.splitArgs(arg, 2);
        if (parts.length != 2) {
            appendArgCountError(lineOutput, 1 + this.expectedPluralCount, parts.length);
            return;
        }

        // validate validate output counts
        List<String> plurals = TemplatedTextLoader
                .splitArgsWithCommands(loader, parts[1], this.expectedPluralCount, false, context);
        if (plurals.size() != this.expectedPluralCount) {
            appendArgCountError(lineOutput, 1 + this.expectedPluralCount, 1 + plurals.size());
            return;
        }

        // parse the number
        CLDRPluralRuleNumber container = getCLDRPluralRuleNumber(context, parts[0]);
        if (container == null) {
            appendError(lineOutput, "VAR IS NOT A NUMBER");
            return;
        }

        // get plurality index
        int index = parser.getIndex(container);

        if (index < 0) return;
        if (index >= plurals.size()) return;
        loader.emit(plurals.get(index), lineOutput, context);
    }
}
