package com.gtnewhorizon.gtnhlib.templatedtext.command;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

public class HorizontalRuleCommand extends TemplatedTextCommand {

    public HorizontalRuleCommand() {
        super("HR");
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {
        // if there are no args, use the default length.
        if (arg.isEmpty()) {
            TextFormatUtil.appendLineSeparator(lineOutput);
            return;
        }

        // else parse the length and append
        int length;
        try {
            length = Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            length = -1;
        }
        if (length < 0) {
            appendError(lineOutput, "NOT AN INT: " + arg);
            return;
        }
        lineOutput.append(TextFormatUtil.getLineSeparator(length));
    }
}
