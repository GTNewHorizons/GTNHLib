package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;

public final class ResourcePackUpdateChecker {

    private static final long FAILURE_COOLDOWN_MILLIS = 30L * 60L * 1000L;
    private static final long MANUAL_COOLDOWN_MILLIS = 30L * 1000L;
    private static boolean hasRunThisSession = false;
    private static long lastFailureMillis = 0L;
    private static long lastManualMillis = 0L;
    private static long lastCheckMillis = 0L;
    private static final Set<String> mismatchNotified = new HashSet<>();
    private static final AtomicBoolean checkInProgress = new AtomicBoolean(false);

    private ResourcePackUpdateChecker() {}

    public static void runAutoCheckIfNeeded() {
        if (hasRunThisSession) {
            return;
        }
        hasRunThisSession = true;
        if (!GTNHLibConfig.enableResourcePackUpdateCheck) {
            RpUpdaterLog.debug("Auto-check skipped (disabled in config)");
            return;
        }
        runCheckAsync(false, false);
    }

    public static CheckResult runManualCheck(boolean force) {
        if (!force && isManualOnCooldown()) {
            ChatNotifier.sendManualCooldown();
            return new CheckResult();
        }
        lastManualMillis = System.currentTimeMillis();
        runCheckAsync(force, true);
        return new CheckResult();
    }

    private static void runCheckAsync(boolean force, boolean isManual) {
        if (!checkInProgress.compareAndSet(false, true)) {
            RpUpdaterLog.debug("Update check already in progress");
            if (isManual) {
                ChatNotifier.sendAlreadyRunning();
            }
            return;
        }
        if (isOnCooldown() && !force) {
            if (isManual) {
                ChatNotifier.sendCooldownMessage();
            }
            RpUpdaterLog.debug("Check skipped due to cooldown");
            checkInProgress.set(false);
            return;
        }
        String playerLine = getPlayerLineNormalized();
        List<IResourcePack> packs = getEnabledPacks();
        if (isManual) {
            ChatNotifier.sendChecking();
        }
        Thread worker = new Thread(() -> runCheckWorker(packs, playerLine, isManual), "GTNHLib-RPUpdater");
        worker.setDaemon(true);
        worker.start();
    }

    private static void runCheckWorker(List<IResourcePack> packs, String playerLine, boolean isManual) {
        try {
            RpUpdaterLog.debug("Starting update check (player line: {})", playerLine);
            CheckResult result = new CheckResult();
            List<UpdaterMeta> metas = scanEnabledPacks(packs, result);
            Map<RepoKey, Optional<ReleaseMatch>> releaseCache = new HashMap<>();
            for (UpdaterMeta meta : metas) {
                checkOnePack(meta, playerLine, releaseCache, result);
            }
            if (isManual && result.updatesFound == 0) {
                ChatNotifier.sendNoUpdatesFound();
            }
        } finally {
            lastCheckMillis = System.currentTimeMillis();
            checkInProgress.set(false);
        }
    }

    private static List<IResourcePack> getEnabledPacks() {
        ResourcePackRepository repo = Minecraft.getMinecraft().getResourcePackRepository();
        List<ResourcePackRepository.Entry> entries = repo.getRepositoryEntries();
        List<IResourcePack> packs = new ArrayList<>(entries.size());
        for (ResourcePackRepository.Entry entry : entries) {
            IResourcePack pack = entry.getResourcePack();
            if (pack == null) {
                continue;
            }
            packs.add(pack);
        }
        return packs;
    }

    private static List<UpdaterMeta> scanEnabledPacks(List<IResourcePack> packs, CheckResult result) {
        result.packsScanned = packs.size();
        List<UpdaterMeta> metas = new ArrayList<>();
        for (IResourcePack pack : packs) {
            Optional<UpdaterMeta> meta = PackMcmetaReader.readUpdaterMeta(pack);
            if (meta.isPresent()) {
                result.packsWithUpdater++;
                metas.add(meta.get());
                RpUpdaterLog.debug("Found updater metadata in pack {}", pack.getPackName());
            }
        }
        RpUpdaterLog.debug("Scanned {} packs ({} with updater metadata)", result.packsScanned, result.packsWithUpdater);
        return metas;
    }

    private static void checkOnePack(UpdaterMeta meta, String playerLine, Map<RepoKey, Optional<ReleaseMatch>> cache,
            CheckResult result) {
        String installedLine = meta.packGameVersion;
        String targetLine = "unknown".equals(playerLine) ? installedLine : playerLine;
        if (!"unknown".equals(playerLine) && !installedLine.equals(playerLine) && mismatchNotified.add(meta.packName)) {
            ChatNotifier.sendLineMismatch(meta.packName, installedLine, playerLine);
        }
        Optional<ReleaseMatch> match = getNewestRelease(meta, targetLine, cache, result);
        if (!match.isPresent()) {
            RpUpdaterLog.debug("No compatible release for {} on {}", meta.packName, targetLine);
            return;
        }
        ReleaseMatch release = match.get();
        try {
            PackVersion installed = PackVersion.parse(meta.packVersion);
            PackVersion remote = PackVersion.parse(release.packVersion);
            if (remote.compareTo(installed) > 0) {
                ChatNotifier.sendUpdateMessage(meta.packName, meta.packVersion, release.packVersion, release.htmlUrl);
                result.updatesFound++;
            } else {
                RpUpdaterLog.debug(
                        "No update for {} (installed {}, remote {})",
                        meta.packName,
                        meta.packVersion,
                        release.packVersion);
            }
        } catch (IllegalArgumentException e) {
            RpUpdaterLog.warn("Invalid pack version for {}: {}", meta.packName, e.toString());
        }
    }

    private static Optional<ReleaseMatch> getNewestRelease(UpdaterMeta meta, String targetLine,
            Map<RepoKey, Optional<ReleaseMatch>> cache, CheckResult result) {
        RepoKey key = new RepoKey(meta.owner, meta.repo, targetLine);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        try {
            Optional<ReleaseMatch> match = GitHubReleaseClient
                    .findNewestCompatibleRelease(meta.owner, meta.repo, targetLine);
            cache.put(key, match);
            return match;
        } catch (Exception e) {
            result.hadFailure = true;
            lastFailureMillis = System.currentTimeMillis();
            RpUpdaterLog.warn("GitHub request failed for {}/{}: {}", meta.owner, meta.repo, e.toString());
            return Optional.empty();
        }
    }

    private static boolean isOnCooldown() {
        if (lastFailureMillis == 0L) {
            return false;
        }
        return System.currentTimeMillis() - lastFailureMillis < FAILURE_COOLDOWN_MILLIS;
    }

    private static boolean isManualOnCooldown() {
        if (lastManualMillis == 0L) {
            return false;
        }
        return System.currentTimeMillis() - lastManualMillis < MANUAL_COOLDOWN_MILLIS;
    }

    private static String getPlayerLineNormalized() {
        String version = readModpackVersion();
        if (version == null) {
            return "unknown";
        }
        if (version.startsWith("2.8")) {
            return "2.8.X";
        }
        if (version.startsWith("2.9")) {
            return "2.9.X";
        }
        if (version.matches("\\d+\\.\\d+\\.X")) {
            return version;
        }
        return "unknown";
    }

    private static String readModpackVersion() { // TODO Get Actual modpack version somehow, because yes
        try {
            Class<?> refstrings = Class.forName("com.dreammaster.lib.Refstrings");
            return (String) refstrings.getField("MODPACKPACK_VERSION").get(null);
        } catch (Exception e) {
            RpUpdaterLog.debug("Refstrings not available: {}", e.toString());
            return null;
        }
    }

    public static final class CheckResult {

        public int packsScanned;
        public int packsWithUpdater;
        public int updatesFound;
        public boolean cooldownBlocked;
        public boolean hadFailure;
    }

    public static List<PackSummary> getActivePackSummaries() {
        List<IResourcePack> packs = getEnabledPacks();
        List<PackSummary> summaries = new ArrayList<>(packs.size());
        for (IResourcePack pack : packs) {
            Optional<UpdaterMeta> meta = PackMcmetaReader.readUpdaterMeta(pack);
            if (meta.isPresent()) {
                UpdaterMeta updater = meta.get();
                summaries.add(
                        new PackSummary(
                                pack.getPackName(),
                                true,
                                updater.packName,
                                updater.packVersion,
                                updater.packGameVersion,
                                updater.owner,
                                updater.repo));
            } else {
                summaries.add(new PackSummary(pack.getPackName(), false, null, null, null, null, null));
            }
        }
        return summaries;
    }

    public static StatusSnapshot getStatusSnapshot() {
        long now = System.currentTimeMillis();
        long failureRemaining = Math.max(0L, (lastFailureMillis + FAILURE_COOLDOWN_MILLIS) - now);
        long manualRemaining = Math.max(0L, (lastManualMillis + MANUAL_COOLDOWN_MILLIS) - now);
        return new StatusSnapshot(checkInProgress.get(), lastCheckMillis, failureRemaining, manualRemaining);
    }

    public static final class PackSummary {

        public final String packDisplayName;
        public final boolean hasUpdater;
        public final String updaterPackName;
        public final String packVersion;
        public final String packGameVersion;
        public final String owner;
        public final String repo;

        private PackSummary(String packDisplayName, boolean hasUpdater, String updaterPackName, String packVersion,
                String packGameVersion, String owner, String repo) {
            this.packDisplayName = packDisplayName;
            this.hasUpdater = hasUpdater;
            this.updaterPackName = updaterPackName;
            this.packVersion = packVersion;
            this.packGameVersion = packGameVersion;
            this.owner = owner;
            this.repo = repo;
        }
    }

    public static final class StatusSnapshot {

        public final boolean running;
        public final long lastCheckMillis;
        public final long failureCooldownRemainingMillis;
        public final long manualCooldownRemainingMillis;

        private StatusSnapshot(boolean running, long lastCheckMillis, long failureCooldownRemainingMillis,
                long manualCooldownRemainingMillis) {
            this.running = running;
            this.lastCheckMillis = lastCheckMillis;
            this.failureCooldownRemainingMillis = failureCooldownRemainingMillis;
            this.manualCooldownRemainingMillis = manualCooldownRemainingMillis;
        }
    }

    private static final class RepoKey {

        private final String owner;
        private final String repo;
        private final String targetLine;

        private RepoKey(String owner, String repo, String targetLine) {
            this.owner = owner;
            this.repo = repo;
            this.targetLine = targetLine;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RepoKey)) {
                return false;
            }
            RepoKey other = (RepoKey) obj;
            return owner.equals(other.owner) && repo.equals(other.repo) && targetLine.equals(other.targetLine);
        }

        @Override
        public int hashCode() {
            int result = owner.hashCode();
            result = 31 * result + repo.hashCode();
            result = 31 * result + targetLine.hashCode();
            return result;
        }
    }
}
