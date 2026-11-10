package com.gtnewhorizon.gtnhlib.api.templatedtext;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

@FunctionalInterface
public interface ITemplatedTextLineProcessor {

    void processLine(@NotNull TemplatedTextLoader loader, @NotNull List<String> out, @NotNull String line,
            @NotNull TemplatedTextContext context);
}
