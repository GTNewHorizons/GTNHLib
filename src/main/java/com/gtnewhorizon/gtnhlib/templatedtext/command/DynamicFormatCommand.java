package com.gtnewhorizon.gtnhlib.templatedtext.command;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextCommand;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.TextFormatUtil;

/// Highlights a segment of text with a dynamically created format code.
///
/// Usage Example:
/// ```text
/// {color_mechanic}this text is colored
/// {color_mechanic:this text is colored} This text is not colored
/// ```
public class DynamicFormatCommand implements ITemplatedTextCommand {

    private final @NotNull Supplier<@NotNull String> formatSupplier;

    public DynamicFormatCommand(@NotNull Supplier<@NotNull String> format) {
        this.formatSupplier = format;
    }

    @Override
    public void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context) {
        if (arg.isEmpty()) {
            lineOutput.append(this.formatSupplier.get());
        } else {
            lineOutput.append(TextFormatUtil.FORMAT_PUSH_STACK).append(formatSupplier.get());
            loader.emit(arg, lineOutput, context);
            lineOutput.append(TextFormatUtil.FORMAT_POP_STACK);
        }
    }
}
