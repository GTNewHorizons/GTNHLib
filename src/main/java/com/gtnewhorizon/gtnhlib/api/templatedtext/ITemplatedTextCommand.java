package com.gtnewhorizon.gtnhlib.api.templatedtext;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

@FunctionalInterface
public interface ITemplatedTextCommand {

    void process(@NotNull TemplatedTextLoader loader, @NotNull String arg, @NotNull StringBuilder lineOutput,
            @NotNull Map<String, Object> vars);

}
