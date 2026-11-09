package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

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
    private final int partCount;

    public PluralPresetCommand(@NotNull CLDRPluralRuleParser parser, @NotNull String name) {
        super("PLURAL-" + name);
        this.parser = parser;
        this.partCount = parser.getRuleCount() + 2;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {
        String[] parts = arg.split(TemplatedTextLoader.ARG_SPLITTER_PATTERN, this.partCount);

        // validate part count
        if (parts.length != this.partCount) {
            appendArgCountError(lineOutput, this.partCount, parts.length);
            return;
        }

        // check if the arg is the correct type
        Object unparsed = vars.get(parts[0]);
        if (!(unparsed instanceof Number number)) {
            appendError(lineOutput, "VAR IS NOT A NUMBER");
            return;
        }

        // get plurality index
        Object format = vars.get(parts[0] + "-fmt");
        CLDRPluralRuleNumber container;
        if (format instanceof NumberOptionsBase<?>options) {
            container = new CLDRPluralRuleNumber(number, options);
        } else {
            container = new CLDRPluralRuleNumber(number, new FormatOptions());
        }
        int index = parser.getIndex(container) + 1;

        if (index <= 0) return;
        if (index >= parts.length) return;
        lineOutput.append(parts[index]);
    }
}
