package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

final class GitHubReleaseClient {

    private static final String USER_AGENT = "GTNHLib-RPUpdater";
    private static final int TIMEOUT_MS = 10_000;
    private static final String ASSET_NAME = "gtnh-pack-update.json";

    private GitHubReleaseClient() {}

    static Optional<ReleaseMatch> findNewestCompatibleRelease(String owner, String repo, String targetLine)
            throws IOException {
        List<ReleaseInfo> releases = fetchReleases(owner, repo);
        for (ReleaseInfo release : releases) {
            ReleaseAsset asset = release.findAsset(ASSET_NAME);
            if (asset == null) {
                continue;
            }
            ReleaseAssetMeta meta = fetchAssetMeta(asset.downloadUrl);
            if (meta == null) {
                continue;
            }
            if (matchesLine(meta.packGameVersion, targetLine)) {
                RpUpdaterLog
                        .debug("Found compatible release {} for {}/{} ({})", release.htmlUrl, owner, repo, targetLine);
                return Optional.of(new ReleaseMatch(meta.packVersion, release.htmlUrl));
            }
        }
        return Optional.empty();
    }

    private static List<ReleaseInfo> fetchReleases(String owner, String repo) throws IOException {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/releases?per_page=20";
        JsonElement element = readJson(url);
        if (!element.isJsonArray()) {
            throw new IOException("Unexpected releases response");
        }
        JsonArray array = element.getAsJsonArray();
        List<ReleaseInfo> releases = new ArrayList<>();
        for (JsonElement releaseElement : array) {
            if (!releaseElement.isJsonObject()) {
                continue;
            }
            JsonObject releaseObj = releaseElement.getAsJsonObject();
            String htmlUrl = readOptionalString(releaseObj, "html_url");
            if (htmlUrl == null) {
                continue;
            }
            List<ReleaseAsset> assets = new ArrayList<>();
            if (releaseObj.has("assets") && releaseObj.get("assets").isJsonArray()) {
                for (JsonElement assetElement : releaseObj.getAsJsonArray("assets")) {
                    if (!assetElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject assetObj = assetElement.getAsJsonObject();
                    String name = readOptionalString(assetObj, "name");
                    String downloadUrl = readOptionalString(assetObj, "browser_download_url");
                    if (name != null && downloadUrl != null) {
                        assets.add(new ReleaseAsset(name, downloadUrl));
                    }
                }
            }
            releases.add(new ReleaseInfo(htmlUrl, assets));
        }
        return releases;
    }

    private static ReleaseAssetMeta fetchAssetMeta(String downloadUrl) throws IOException {
        JsonElement element = readJson(downloadUrl);
        if (!element.isJsonObject()) {
            throw new IOException("Unexpected asset response");
        }
        JsonObject obj = element.getAsJsonObject();
        int schema = readOptionalInt(obj, "schema", -1);
        if (schema != 1) {
            RpUpdaterLog.warn("Unsupported asset schema {} at {}", schema, downloadUrl);
            return null;
        }
        String packGameVersion = readOptionalString(obj, "pack_game_version");
        String packVersion = readOptionalString(obj, "pack_version");
        if (packGameVersion == null || packVersion == null) {
            RpUpdaterLog.warn("Missing fields in asset metadata at {}", downloadUrl);
            return null;
        }
        return new ReleaseAssetMeta(packGameVersion, packVersion);
    }

    private static JsonElement readJson(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " for " + url);
        }
        try (InputStream stream = connection.getInputStream()) {
            return new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } finally {
            connection.disconnect();
        }
    }

    private static String readOptionalString(JsonObject obj, String field) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) {
            return null;
        }
        return obj.get(field).getAsString();
    }

    private static int readOptionalInt(JsonObject obj, String field, int def) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) {
            return def;
        }
        return obj.get(field).getAsInt();
    }

    private static boolean matchesLine(String field, String targetLine) {
        if (field == null || targetLine == null) {
            return false;
        }
        for (String part : field.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && trimmed.equals(targetLine)) {
                return true;
            }
        }
        return false;
    }

    private static final class ReleaseInfo {

        final String htmlUrl;
        final List<ReleaseAsset> assets;

        ReleaseInfo(String htmlUrl, List<ReleaseAsset> assets) {
            this.htmlUrl = htmlUrl;
            this.assets = assets;
        }

        ReleaseAsset findAsset(String name) {
            for (ReleaseAsset asset : assets) {
                if (name.equals(asset.name)) {
                    return asset;
                }
            }
            return null;
        }
    }

    private static final class ReleaseAsset {

        final String name;
        final String downloadUrl;

        ReleaseAsset(String name, String downloadUrl) {
            this.name = name;
            this.downloadUrl = downloadUrl;
        }
    }

    private static final class ReleaseAssetMeta {

        final String packGameVersion;
        final String packVersion;

        ReleaseAssetMeta(String packGameVersion, String packVersion) {
            this.packGameVersion = packGameVersion;
            this.packVersion = packVersion;
        }
    }
}
