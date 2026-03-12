package com.gtnewhorizon.gtnhlib.client.ResourcePackDarkModeFix;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtnewhorizon.gtnhlib.GTNHLib;

public class DarkModeFixResourceListener implements IResourceManagerReloadListener {

    private static final String METADATA_KEY = "gtnh_resource_pack_darkmode_text_fix";
    private static final String[] INPUT_STREAM_METHODS = { "getInputStreamByName", "func_110591_a" };

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        DarkModeFixController.clearColorCache();
        List<IResourcePack> packs = getEnabledPacks();
        GTNHLib.LOG.debug("[DarkModeFix] Scan starting (packs: {})", packs.size());

        for (int i = packs.size() - 1; i >= 0; i--) {
            IResourcePack pack = packs.get(i);
            try {
                Optional<DarkModeFixConfig> config = readConfig(pack);
                if (config.isPresent()) {
                    GTNHLib.LOG.info("[DarkModeFix] Enabled from resource pack: {}", pack.getPackName());
                    DarkModeFixController.enable(config.get());
                    return;
                }
            } catch (RuntimeException e) {
                GTNHLib.LOG.warn("[DarkModeFix] Invalid metadata in pack {}: {}", pack.getPackName(), e.getMessage());
            }
        }

        // No pack contained metadata
        DarkModeFixController.disable();
        GTNHLib.LOG.info("[DarkModeFix] Disabled (no metadata found)");
    }

    private static List<IResourcePack> getEnabledPacks() {
        ResourcePackRepository repo = Minecraft.getMinecraft().getResourcePackRepository();
        List<ResourcePackRepository.Entry> entries = repo.getRepositoryEntries();
        List<IResourcePack> packs = new ArrayList<>(entries.size());
        for (ResourcePackRepository.Entry entry : entries) {
            IResourcePack pack = entry.getResourcePack();
            if (pack != null) {
                packs.add(pack);
            }
        }
        return packs;
    }

    private static Optional<DarkModeFixConfig> readConfig(IResourcePack pack) {
        try (InputStream stream = openPackMcmeta(pack)) {
            if (stream == null) {
                GTNHLib.LOG.debug("[DarkModeFix] Unable to open pack.mcmeta for pack {}", pack.getPackName());
                return Optional.empty();
            }
            JsonElement rootElement = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!rootElement.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has(METADATA_KEY)) {
                return Optional.empty();
            }
            if (!root.get(METADATA_KEY).isJsonObject()) {
                throw new IllegalArgumentException(
                        "DarkModeFix metadata is not an object in pack " + pack.getPackName());
            }
            JsonObject meta = root.getAsJsonObject(METADATA_KEY);
            int schema = readRequiredInt(meta, "schema", pack.getPackName());
            if (schema != 1) {
                throw new IllegalArgumentException(
                        "Unsupported DarkModeFix schema " + schema + " in pack " + pack.getPackName());
            }
            float darkThreshold = readRequiredFloat(meta, "dark_threshold", pack.getPackName());
            float minBrightness = readRequiredFloat(meta, "min_brightness", pack.getPackName());
            float maxBrightness = readRequiredFloat(meta, "max_brightness", pack.getPackName());
            return Optional.of(new DarkModeFixConfig(darkThreshold, minBrightness, maxBrightness));
        } catch (IOException e) {
            GTNHLib.LOG.debug("[DarkModeFix] Failed reading pack.mcmeta for pack {}", pack.getPackName(), e);
            return Optional.empty();
        }
    }

    private static InputStream openPackMcmeta(IResourcePack pack) throws IOException {
        Method method = findPackMcmetaInputStreamMethod(pack.getClass());
        if (method == null) {
            return null;
        }
        try {
            return (InputStream) method.invoke(pack, "pack.mcmeta");
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Method findPackMcmetaInputStreamMethod(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            for (String name : INPUT_STREAM_METHODS) {
                try {
                    Method method = current.getDeclaredMethod(name, String.class);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                    // Try other names or fallback scan.
                }
            }
            for (Method method : current.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0] == String.class
                        && InputStream.class.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static int readRequiredInt(JsonObject obj, String field, String packName) {
        if (!obj.has(field)) {
            throw new IllegalArgumentException("Missing " + field + " in pack " + packName);
        }
        return obj.get(field).getAsInt();
    }

    private static float readRequiredFloat(JsonObject obj, String field, String packName) {
        if (!obj.has(field)) {
            throw new IllegalArgumentException("Missing " + field + " in pack " + packName);
        }
        return obj.get(field).getAsFloat();
    }
}
