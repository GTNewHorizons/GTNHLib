package com.gtnewhorizon.gtnhlib.templatedtext.command;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

public class UnitCommand extends TemplatedTextCommand {

    protected final @NotNull NumberFormatUtil.IUnitFormatter formatter;

    public UnitCommand(@NotNull NumberFormatUtil.IUnitFormatter formatter) {
        super("PLAIN-UNIT");
        this.formatter = formatter;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {
        if (arg.isEmpty()) {
            lineOutput.append(formatter.getUnit());
        } else {
            // find var
            Object value = context.getVar(arg);
            if (value == null) {
                appendUnknownVarError(lineOutput, arg);
                return;
            }

            // format var
            String formatted;
            if (value instanceof Number v) {
                StringBuilder sb = new StringBuilder();
                VarCommand.appendNumericVar(v, arg, sb, context);
                formatted = this.formatter.format(sb.toString());
            } else {
                formatted = this.formatter.format(value.toString());
            }

            // append measurement
            lineOutput.append(formatted);
        }
    }

}
