package com.gtnewhorizon.gtnhlib.api.templatedtext;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;

@FunctionalInterface
public interface ITemplatedTextLineProcessor {

    void processLine(@NotNull TemplatedTextLoader loader, @NotNull List<String> out, @NotNull String line,
            @NotNull Map<String, Object> vars, Stack<ResourceLocation> includeHistory);
}
