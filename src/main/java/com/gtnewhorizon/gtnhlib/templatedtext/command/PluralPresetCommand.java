package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.List;
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
    private final int expectedPluralCount;

    public PluralPresetCommand(@NotNull CLDRPluralRuleParser parser, @NotNull String name) {
        super("PLURAL-" + name);
        this.parser = parser;
        this.expectedPluralCount = parser.getRuleCount() + 1;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {
        String[] parts = TemplatedTextLoader.splitArgs(arg, 2);
        // validate part count
        if (parts.length != 2) {
            appendArgCountError(lineOutput, 1 + this.expectedPluralCount, parts.length);
            return;
        }
        List<String> plurals = TemplatedTextLoader
                .splitArgsWithCommands(loader, parts[1], this.expectedPluralCount, true, vars);
        // validate validate output counts
        if (plurals.size() != this.expectedPluralCount) {
            appendArgCountError(lineOutput, 1 + this.expectedPluralCount, 1 + plurals.size());
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
        int index = parser.getIndex(container);

        if (index < 0) return;
        if (index >= plurals.size()) return;
        lineOutput.append(plurals.get(index));
    }
}
