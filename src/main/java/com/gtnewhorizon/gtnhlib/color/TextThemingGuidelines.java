package com.gtnewhorizon.gtnhlib.color;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public abstract class TextThemingGuidelines implements AutoCloseable {

    private static final String COMMENT_START = "#";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String ASSET_FOLDER = "textthemingguidelines/";

    private static final class FormatKey {

        private final @NotNull String def;
        private final @NotNull Consumer<@NotNull String> setter;

        private FormatKey(@NotNull String def, @NotNull Consumer<@NotNull String> setter) {
            this.def = def;
            this.setter = setter;
        }
    }

    /**
     * Used to clear the cache when resources are reloaded.
     */
    @SideOnly(Side.CLIENT)
    private static class ReloadListener implements IResourceManagerReloadListener {

        private static final ReloadListener INSTANCE = new ReloadListener();

        @Override
        public void onResourceManagerReload(IResourceManager manager) {
            for (TextThemingGuidelines container : INSTANCES) {
                INSTANCES.forEach(x -> x.reload(manager));
            }
        }

        private ReloadListener() {

        }
    }

    private static final ConcurrentSet<TextThemingGuidelines> INSTANCES = new ConcurrentSet<>();

    @ApiStatus.Internal
    @SideOnly(Side.CLIENT)
    public static IResourceManagerReloadListener getReloadListener() {
        return ReloadListener.INSTANCE;
    }

    @SideOnly(Side.CLIENT)
    private void reload(IResourceManager manager) {
        Map<String, String> values = new Object2ObjectOpenHashMap<>();

        // read theming files associated with this container
        for (ResourceLocation resource : resourceLocations) {
            List<String> lines;
            try {
                IResource cfgResource = manager.getResource(resource);
                lines = IOUtils.readLines(cfgResource.getInputStream());
            } catch (IOException e) {
                GTNHLib.LOG.error("failed to open text coloring file", e);
                return;
            }

            reload(lines, values);
        }

        // update values based on registered handlers.
        for (Object2ObjectMap.Entry<@NotNull String, @NotNull FormatKey> entry : this.keyHandlers
                .object2ObjectEntrySet()) {
            entry.getValue().setter.accept(values.getOrDefault(entry.getKey(), entry.getValue().def));
        }
    }

    @VisibleForTesting
    public void reload(@NotNull List<@NotNull String> lines, Map<String, String> outputMap) {
        // skip lines with comments or those that are empty
        lines.removeIf(l -> l.startsWith(COMMENT_START) || l.isEmpty());

        // parse keys
        for (String line : lines) {
            // abort if no split
            int splitIndex = line.indexOf(KEY_VALUE_SEPARATOR);
            if (splitIndex < 0) continue;
            outputMap.put(line.substring(0, splitIndex).trim(), line.substring(splitIndex + 1).trim());
        }
    }

    private final @NotNull ResourceLocation[] resourceLocations;
    private final @NotNull Object2ObjectOpenHashMap<@NotNull String, @NotNull FormatKey> keyHandlers = new Object2ObjectOpenHashMap<>();

    public TextThemingGuidelines(@NotNull ResourceLocation @NotNull... resourceLocations) {
        this.resourceLocations = new ResourceLocation[resourceLocations.length];
        for (int i = 0; i < this.resourceLocations.length; i++) {
            this.resourceLocations[i] = new ResourceLocation(
                    resourceLocations[i].getResourceDomain(),
                    ASSET_FOLDER + resourceLocations[i].getResourcePath());
        }
        INSTANCES.add(this);
    }

    @VisibleForTesting
    @Override
    public void close() throws Exception {
        INSTANCES.remove(this);
        this.keyHandlers.clear();
    }

    /**
     * Registers a key handler for a given key.
     *
     * @param name      The name of the key.
     * @param defGetter A lazy resolver for the default value.
     * @param setter    The function to call when updating this value.
     */
    protected void registerKey(String name, Supplier<@NotNull String> defGetter, Consumer<@NotNull String> setter) {
        this.registerKey(name, defGetter.get(), setter);
    }

    /**
     * Registers a key handler for a given key.
     *
     * @param name   The name of the key.
     * @param def    The default value of the key.
     * @param setter The function to call when updating this value.
     */
    protected void registerKey(@NotNull String name, @NotNull String def, @NotNull Consumer<@NotNull String> setter) {
        this.keyHandlers.put(name, new FormatKey(def, setter));
        setter.accept(def);
    }
}
