package com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Only renders the line if the given variable is defined (or doesn't)
///
/// Usage Examples:
/// ```text
/// #ifdef:var:This line only appears if var has been defined
/// #ifndef:var:This line only appears if var has not been defined
/// ```
public class IfDefLineProcessor implements ITemplatedTextLineProcessor {

    private final boolean wantsExists;

    public IfDefLineProcessor(boolean wantsExists) {
        this.wantsExists = wantsExists;
    }

    @Override
    public void processLine(@NotNull TemplatedTextLoader loader, @NotNull List<String> out, @NotNull String line,
            @NotNull Map<String, Object> vars, Stack<ResourceLocation> includeHistory) {
        String[] parts = line.split(TemplatedTextLoader.ARG_SPLITTER_PATTERN, 2);

        // must have 2 parts
        if (parts.length != 2) {
            throw TemplatedTextLoader.MarkdownTooltipParsingException
                    .lineProcessorStandard("IF-EXIST", "MUST HAVE 2 ARGS");
        }

        // check if the arg is the correct type
        if (vars.containsKey(parts[0]) == wantsExists) {
            loader.evaluateLinesNoFormat(out, Collections.singletonList(parts[1]), vars, includeHistory);
        }

    }
}
