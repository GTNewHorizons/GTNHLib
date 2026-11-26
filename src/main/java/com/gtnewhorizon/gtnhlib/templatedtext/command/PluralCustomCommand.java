package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.parsing.CLDRPluralRuleNumber;
import com.gtnewhorizon.gtnhlib.util.parsing.CLDRPluralRuleParser;

/// Allows translators to specify and use a predefined [CLDR Plural
/// Rule](https://www.unicode.org/cldr/charts/43/supplemental/language_plural_rules.html)
/// using the [CLDR Plural Rule Syntax](https://unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules) in
/// the event that the presets supplied by GTNH Lib are either incorrect or insufficient.
///
/// Usage Examples:
/// ```text
/// {var:var_name} {plural-custom:var_name:1:n = 1:one:other}
/// {var:var_name} {plural-custom:var_name:3:n = 1;i % 10 = 2..4:i % 10 = 5..9:one:few:many:other}
/// {var:var_name} {plural-custom:var_name} корпус{plural-uk:var_name:день:дні:днів:дня}
/// ```
public class PluralCustomCommand extends TemplatedTextCommand {

    public PluralCustomCommand() {
        super("PLURAL-CUSTOM");
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {
        String[] parts = TemplatedTextLoader.splitArgs(arg, 3);

        // validate part count
        if (parts.length != 3) {
            // smallest arg list: var:0:other
            appendMinArgCountError(lineOutput, 3, parts.length);
            return;
        }

        // parse number var
        CLDRPluralRuleNumber container = PluralPresetCommand.getCLDRPluralRuleNumber(context, parts[0]);
        if (container == null) {
            appendError(lineOutput, "VAR IS NOT A NUMBER");
            return;
        }

        // parse rule count
        int ruleCount;
        if (!StringUtils.isNumeric(parts[1])) {
            ruleCount = -1;
        } else {
            try {
                ruleCount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException nfe) {
                ruleCount = -1;
            }
        }
        if (ruleCount < 0) {
            appendError(lineOutput, "FIRST ARG IS NOT AN INT: " + parts[1]);
            return;
        }

        // split rules and outputs
        // +1 for the other fallback
        List<String> rulesAndOutputs = TemplatedTextLoader
                .splitArgsWithCommands(loader, parts[2], ruleCount * 2 + 1, false, context);
        if (rulesAndOutputs.size() != ruleCount * 2 + 1) {
            // +2 for the rule count and var arg
            appendArgCountError(lineOutput, ruleCount * 2 + 3, rulesAndOutputs.size() + 1);
            return;
        }

        // abort early for worryingly simple rules
        if (ruleCount == 0) {
            loader.emit(rulesAndOutputs.get(rulesAndOutputs.size() - 1), lineOutput, context);
            return;
        }

        // create parser
        CLDRPluralRuleParser parser;
        try {
            parser = CLDRPluralRuleParser.getParser(rulesAndOutputs.subList(0, ruleCount).toArray(new String[0]));
        } catch (CLDRPluralRuleParser.PluralityRuleParsingException err) {
            appendError(lineOutput, err.getMessage());
            return;
        }

        // evaluate outcome
        int index = ruleCount + parser.getIndex(container);
        if (index < ruleCount) return;
        if (index >= rulesAndOutputs.size()) return;
        loader.emit(rulesAndOutputs.get(index), lineOutput, context);
    }
}
