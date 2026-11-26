package com.gtnewhorizon.gtnhlib.templatedtext;

import java.util.Map;
import java.util.Stack;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMap;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class TemplatedTextContext {

    final @NotNull ImmutableMap<@NotNull String, @Nullable Object> vars;
    final @NotNull Stack<@NotNull ResourceLocation> includeHistory;
    public final @NotNull Map<@NotNull String, @Nullable Object> store = new Object2ObjectOpenHashMap<>();

    public TemplatedTextContext() {
        this(ImmutableMap.of());
    }

    public TemplatedTextContext(@NotNull Stack<@NotNull ResourceLocation> includeHistory) {
        this(ImmutableMap.of(), includeHistory);
    }

    public TemplatedTextContext(@NotNull Map<@NotNull String, @Nullable Object> vars) {
        this(vars, new Stack<>());
    }

    public TemplatedTextContext(@NotNull Map<@NotNull String, @Nullable Object> vars,
            @NotNull Stack<@NotNull ResourceLocation> includeHistory) {
        this(vars instanceof ImmutableMap<String, Object>im ? im : ImmutableMap.copyOf(vars), includeHistory);
    }

    public TemplatedTextContext(@NotNull ImmutableMap<@NotNull String, @Nullable Object> vars) {
        this(vars, new Stack<>());
    }

    public TemplatedTextContext(@NotNull ImmutableMap<@NotNull String, @Nullable Object> vars,
            @NotNull Stack<@NotNull ResourceLocation> includeHistory) {
        this.vars = vars;
        this.includeHistory = includeHistory;
    }

    public @Nullable Object getVar(@NotNull String varName) {
        return vars.get(varName);
    }

    public <T> @Nullable T getVar(@NotNull String varName, @NotNull Class<T> t) {
        Object var = vars.get(varName);
        if (var == null) return null;
        return t.isInstance(var) ? t.cast(var) : null;
    }

    public @Nullable Object getVarOrDefault(@NotNull String varName, @Nullable Object def) {
        return vars.getOrDefault(varName, def);
    }

    public <T> @Nullable T getVarOrDefault(@NotNull String varName, @Nullable T def, @NotNull Class<T> t) {
        if (!this.vars.containsKey(varName)) return def;
        Object var = vars.get(varName);
        return t.isInstance(var) ? t.cast(var) : def;
    }

    public boolean containsVar(@NotNull String varName) {
        return vars.containsKey(varName);
    }

    public <T> boolean containsVar(@NotNull String varName, @NotNull Class<T> t) {
        Object var = vars.get(varName);
        if (var == null) return false;
        return t.isInstance(var);
    }

    public boolean push(@NotNull ResourceLocation resource) {
        for (ResourceLocation rs : this.includeHistory) {
            if (rs.equals(resource)) {
                return false;
            }
        }
        this.includeHistory.push(resource);
        return true;
    }

    public void pop() {
        this.includeHistory.pop();
    }
}
