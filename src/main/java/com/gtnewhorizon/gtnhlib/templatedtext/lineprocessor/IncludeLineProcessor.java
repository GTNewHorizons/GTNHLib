package com.gtnewhorizon.gtnhlib.templatedtext.lineprocessor;

import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.api.templatedtext.ITemplatedTextLineProcessor;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

/// Evaluates the content of another templated text file under the same root folder as the calling file and appends it.
///
/// Usage Example:
/// ```text
/// #include:modid:template
/// ```
public class IncludeLineProcessor implements ITemplatedTextLineProcessor {

    @Override
    public void processLine(@NotNull TemplatedTextLoader loader, @NotNull List<String> lineOutput, @NotNull String line,
            @NotNull TemplatedTextContext context) {
        ResourceLocation loc = new ResourceLocation(line.trim());
        lineOutput.addAll(loader.loadStandardPathNoFormat(loc, context));
    }
}
