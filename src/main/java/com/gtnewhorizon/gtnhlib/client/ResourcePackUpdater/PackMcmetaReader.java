package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.minecraft.client.resources.IResourcePack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class PackMcmetaReader {

    private static final String UPDATER_KEY = "gtnh_resource_pack_updater";
    private static final String SOURCE_TYPE_GITHUB = "github_releases";
    private static final String[] INPUT_STREAM_METHODS = { "getInputStreamByName", "func_110591_a" };

    private PackMcmetaReader() {}

    static Optional<UpdaterMeta> readUpdaterMeta(IResourcePack pack) {
        try (InputStream stream = openPackMcmeta(pack)) {
            if (stream == null) {
                return Optional.empty();
            }
            JsonElement rootElement = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!rootElement.isJsonObject()) {
                RpUpdaterLog.warn("pack.mcmeta root is not an object for pack {}", pack.getPackName());
                return Optional.empty();
            }
            JsonObject root = rootElement.getAsJsonObject();
            if (!root.has(UPDATER_KEY) || !root.get(UPDATER_KEY).isJsonObject()) {
                return Optional.empty();
            }
            JsonObject updater = root.getAsJsonObject(UPDATER_KEY);
            int schema = readRequiredInt(updater, "schema", pack.getPackName());
            if (schema != 1) {
                RpUpdaterLog.warn("Unsupported updater schema {} in pack {}", schema, pack.getPackName());
                return Optional.empty();
            }
            String packName = readRequiredString(updater, "pack_name", pack.getPackName());
            String packVersion = readRequiredString(updater, "pack_version", pack.getPackName());
            String packGameVersion = readRequiredString(updater, "pack_game_version", pack.getPackName());
            if (!updater.has("source") || !updater.get("source").isJsonObject()) {
                RpUpdaterLog.warn("Missing source object in pack {}", pack.getPackName());
                return Optional.empty();
            }
            JsonObject source = updater.getAsJsonObject("source");
            String sourceType = readRequiredString(source, "type", pack.getPackName());
            if (!SOURCE_TYPE_GITHUB.equals(sourceType)) {
                RpUpdaterLog.warn("Unsupported source type {} in pack {}", sourceType, pack.getPackName());
                return Optional.empty();
            }
            String owner = readRequiredString(source, "owner", pack.getPackName());
            String repo = readRequiredString(source, "repo", pack.getPackName());
            return Optional.of(new UpdaterMeta(packName, packVersion, packGameVersion, sourceType, owner, repo));
        } catch (IOException e) {
            RpUpdaterLog.warn("Failed reading pack.mcmeta for pack {}: {}", pack.getPackName(), e.toString());
            return Optional.empty();
        } catch (RuntimeException e) {
            RpUpdaterLog.warn("Invalid pack.mcmeta for pack {}: {}", pack.getPackName(), e.toString());
            return Optional.empty();
        }
    }

    private static InputStream openPackMcmeta(IResourcePack pack) throws IOException {
        Method method = findGetInputStreamByName(pack.getClass());
        if (method == null) {
            RpUpdaterLog.warn("Unable to read pack.mcmeta for pack {} (no access method)", pack.getPackName());
            return null;
        }
        try {
            return (InputStream) method.invoke(pack, "pack.mcmeta");
        } catch (Exception e) {
            RpUpdaterLog.warn("Failed opening pack.mcmeta for pack {}: {}", pack.getPackName(), e.toString());
            return null;
        }
    }

    private static Method findGetInputStreamByName(Class<?> type) {
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

    private static String readRequiredString(JsonObject obj, String field, String packName) {
        if (!obj.has(field)) {
            throw new IllegalArgumentException("Missing " + field + " in pack " + packName);
        }
        return obj.get(field).getAsString();
    }

    private static int readRequiredInt(JsonObject obj, String field, String packName) {
        if (!obj.has(field)) {
            throw new IllegalArgumentException("Missing " + field + " in pack " + packName);
        }
        return obj.get(field).getAsInt();
    }
}
