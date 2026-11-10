package com.gtnewhorizon.gtnhlib.color;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.util.internal.ConcurrentSet;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public abstract class TextThemingGuidelineContainer {

    private static final String COMMENT_START = "#";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String ASSET_FOLDER = "textthemingguidelines/";

    private static final class FormatKey {

        private final @NotNull String def;
        private final @NotNull Supplier<@NotNull String> getter;
        private final @NotNull Consumer<@NotNull String> setter;

        private FormatKey(@NotNull Supplier<@NotNull String> getter, @NotNull Consumer<@NotNull String> setter) {
            this.def = getter.get();
            this.setter = setter;
            this.getter = getter;
        }
    }

    /**
     * Used to clear the cache when resources are reloaded.
     */
    @SideOnly(Side.CLIENT)
    private static class ReloadListener implements IResourceManagerReloadListener {

        private static final ReloadListener INSTANCE = new ReloadListener();

        private static final ConcurrentSet<TextThemingGuidelineContainer> INSTANCES = new ConcurrentSet<>();

        @Override
        public void onResourceManagerReload(IResourceManager manager) {
            INSTANCES.forEach(x -> x.reload(manager));
        }

        private ReloadListener() {

        }
    }

    @SideOnly(Side.CLIENT)
    public void onClientConstruct() {
        ReloadListener.INSTANCES.add(this);
    }

    @ApiStatus.Internal
    @SideOnly(Side.CLIENT)
    public static IResourceManagerReloadListener getReloadListener() {
        return ReloadListener.INSTANCE;
    }

    @SideOnly(Side.CLIENT)
    private void reload(IResourceManager manager) {
        Map<String, String> values = new Object2ObjectOpenHashMap<>();

        // read theming files associated with this container
        for (ResourceLocation resource : this.resourceLocations) {
            try {
                IResource cfgResource = manager.getResource(resource);
                try (InputStream is = cfgResource.getInputStream()) {
                    parseResource(values, is);
                }
            } catch (IOException e) {
                GTNHLib.LOG.error("failed to open text coloring file", e);
                return;
            }
        }

        // update the guidelines with the new values
        this.inject(values);
    }

    @VisibleForTesting
    public static void parseResource(@NotNull Map<@NotNull String, @NotNull String> outputMap, @NotNull InputStream is)
            throws IOException {
        List<String> lines = IOUtils.readLines(is, StandardCharsets.UTF_8);
        parseFields(outputMap, lines);
    }

    @VisibleForTesting
    public static void parseFields(@NotNull Map<@NotNull String, @NotNull String> outputMap,
            @NotNull Iterable<@NotNull String> lines) {
        // parse keys
        for (String line : lines) {
            // skip lines with comments or those that are empty
            if (line.isEmpty() || line.startsWith(COMMENT_START)) continue;
            // skip if no split
            int splitIndex = line.indexOf(KEY_VALUE_SEPARATOR);
            if (splitIndex < 0) continue;
            outputMap.put(line.substring(0, splitIndex).trim(), line.substring(splitIndex + 1).trim());
        }
    }

    @VisibleForTesting
    public void inject(@NotNull Map<@NotNull String, @NotNull String> values) {
        // update values based on registered handlers.
        for (Map.Entry<@NotNull String, @NotNull FormatKey> entry : this.keyHandlers.entrySet()) {
            String newValue = values.getOrDefault(entry.getKey(), entry.getValue().def);
            entry.getValue().setter.accept(newValue);
        }
    }

    private final @NotNull ResourceLocation[] resourceLocations;
    private final @NotNull Map<@NotNull String, @NotNull FormatKey> keyHandlers = new ConcurrentHashMap<>();

    public Stream<Pair<String, Supplier<String>>> getEntries() {
        return this.keyHandlers.entrySet().stream().map(x -> Pair.of(x.getKey(), x.getValue().getter));
    }

    public TextThemingGuidelineContainer(@NotNull ResourceLocation @NotNull... resourceLocations) {
        this.resourceLocations = new ResourceLocation[resourceLocations.length];
        for (int i = 0; i < this.resourceLocations.length; i++) {
            this.resourceLocations[i] = new ResourceLocation(
                    resourceLocations[i].getResourceDomain(),
                    ASSET_FOLDER + resourceLocations[i].getResourcePath());
        }
    }

    /**
     * Registers a key handler for a given key.
     *
     * @param name   The name of the key.
     * @param getter A lazy resolver for the default value.
     * @param setter The function to call when updating this value.
     */
    protected void registerKey(String name, Supplier<@NotNull String> getter, Consumer<@NotNull String> setter) {
        this.keyHandlers.put(name, new FormatKey(getter, setter));
    }
}
