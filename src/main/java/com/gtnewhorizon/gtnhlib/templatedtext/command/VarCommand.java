package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.gtnewhorizon.gtnhlib.util.numberformatting.options.NumberOptionsBase;

/// Parses a given variable and optionally parses it's contents for more markdown commands.
///
/// Usage Examples:
/// ```text
/// {var:eff_increase}% increase efficiency!
/// {var-eval:var_that_contains_templated_text}% increase efficiency!
/// ```
public class VarCommand extends TemplatedTextCommand {

    public final boolean eval;

    public VarCommand(boolean eval) {
        super("VAR");
        this.eval = eval;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {

        Object value = vars.get(arg);

        if (value == null) {
            appendUnknownVarError(lineOutput, arg);
            return;
        }

        if (eval) {
            loader.emit(value.toString(), lineOutput, vars);
        } else {
            if (value instanceof Number v) {
                appendNumericVar(v, arg, lineOutput, vars);
            } else {
                lineOutput.append(value);
            }
        }
    }

    public static void appendNumericVar(@NotNull Number number, @NotNull String varName,
            @NotNull StringBuilder lineOutput, @NotNull Map<String, Object> vars) {
        Object maybeFormat = vars.get(varName + "-fmt");
        if (maybeFormat instanceof NumberOptionsBase<?>options) {
            lineOutput.append(options.format(number));
        } else {
            lineOutput.append(NumberFormatUtil.formatNumber(number));
        }
    }
}
