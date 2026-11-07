package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

/// Highlights a segment of text with a dynamically created format code.
///
/// Usage Examples:
/// ```text
/// {red}this text is colored
/// {red:this text is colored} This text is not colored
/// ```
public class StaticFormatCommand implements ITemplatedTextCommand {

    private final @NotNull String format;
    private final @NotNull String push;

    public StaticFormatCommand(@NotNull String format) {
        this.format = format;
        this.push = TextFormatUtil.FORMAT_PUSH_STACK + this.format;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars) {
        if (arg.isEmpty()) {
            lineOutput.append(format);
        } else {
            lineOutput.append(push);
            loader.emit(arg, lineOutput, vars);
            lineOutput.append(TextFormatUtil.FORMAT_POP_STACK);
        }
    }
}
