package com.gtnewhorizon.gtnhlib.api.templatedtext;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextContext;
import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

@FunctionalInterface
public interface ITemplatedTextCommand {

    void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull TemplatedTextContext context);

}
