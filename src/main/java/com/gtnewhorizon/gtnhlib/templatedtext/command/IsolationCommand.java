package com.gtnewhorizon.gtnhlib.templatedtext.command;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

/// Isolates any formatting changes to the contents of the tag.
///
/// NOTE: Color codes disables previous style codes, this is a feature of MC not a bug with the code.
///
/// Example Usage:
/// ```text
/// normal {yellow:yellow {italic}yellow italic {isolate:yellow italic {green}green, {italic}green italic, {blue}blue}
/// yellow italic} normal
/// ```
public class IsolationCommand implements ITemplatedTextCommand {

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {
        if (arg.isEmpty()) return;
        lineOutput.append(TextFormatUtil.FORMAT_PUSH_STACK);
        loader.emit(arg, lineOutput, context);
        lineOutput.append(TextFormatUtil.FORMAT_POP_STACK);
    }
}
