package com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Only renders the line if the given variable is true (or false)
///
/// Usage Examples:
/// ```text
/// #if:boolVar:This line only appears if boolVar is true
/// #if-not:boolVar:This line only appears if boolVar is false
/// ```
public class IfLineProcessor implements ITemplatedTextLineProcessor {

    private final boolean wantsTrue;

    public IfLineProcessor(boolean wantsTrue) {
        this.wantsTrue = wantsTrue;
    }

    @Override
    public void processLine(@NotNull TemplatedTextLoader loader, @NotNull List<String> out, @NotNull String line,
            @NotNull TemplatedTextContext context) {
        String[] parts = line.split(TemplatedTextLoader.ARG_SPLITTER_PATTERN, 2);

        // must have 2 parts
        if (parts.length != 2) {
            throw TemplatedTextLoader.TemplatedTextParsingException.lineProcessorStandard("IF", "MUST HAVE 2 ARGS");
        }

        // check if the arg is the correct type
        if (!(context.getVar(parts[0]) instanceof Boolean value)) {
            throw TemplatedTextLoader.TemplatedTextParsingException
                    .lineProcessorStandard("IF", "UNKNOWN VAR" + parts[0]);
        }

        // check if it's a valid value
        if (value == this.wantsTrue) {
            loader.evaluateLinesNoFormat(out, Collections.singletonList(parts[1]), context);
        }
    }
}
