package com.gtnewhorizon.gtnhlib.client.resourcepackutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

public final class Dumper {

    private static final long CONFIRM_TIMEOUT_MILLIS = 60_000L;
    private static final AtomicBoolean busy = new AtomicBoolean(false);
    private static volatile PendingDump pending;

    private Dumper() {}

    public static void requestScan() {
        if (!busy.compareAndSet(false, true)) {
            ChatNotifier.sendDumpAlreadyRunning();
            return;
        }
        ChatNotifier.sendDumpScanning();
        Thread worker = new Thread(Dumper::runScan, "GTNHLib-resourcepackutils-dump");
        worker.setDaemon(true);
        worker.start();
    }

    public static void requestConfirm() {
        PendingDump plan = pending;
        if (plan == null) {
            ChatNotifier.sendDumpNoPending();
            return;
        }
        if (System.currentTimeMillis() - plan.createdAtMillis > CONFIRM_TIMEOUT_MILLIS) {
            pending = null;
            ChatNotifier.sendDumpExpired();
            return;
        }
        if (!busy.compareAndSet(false, true)) {
            ChatNotifier.sendDumpAlreadyRunning();
            return;
        }
        pending = null;
        Thread worker = new Thread(() -> runCopy(plan), "GTNHLib-resourcepackutils-dump-copy");
        worker.setDaemon(true);
        worker.start();
    }

    private static void runScan() {
        try {
            List<ModAssets> modAssets = scanAllMods();
            int fileCount = 0;
            long totalBytes = 0L;
            for (ModAssets mod : modAssets) {
                fileCount += mod.paths.size();
                totalBytes += mod.totalBytes;
            }
            pending = new PendingDump(modAssets, System.currentTimeMillis());
            ChatNotifier.sendDumpSummary(modAssets.size(), fileCount, totalBytes);
        } catch (Exception e) {
            Log.warn("Dump scan failed: {}", e.toString());
            ChatNotifier.sendDumpScanFailed();
        } finally {
            busy.set(false);
        }
    }

    private static void runCopy(PendingDump plan) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File resourcepacksDir = new File(Minecraft.getMinecraft().mcDataDir, "resourcepacks");
            File outputDir = new File(resourcepacksDir, "ResourcePackDump_" + timestamp);

            int copied = 0;
            for (ModAssets mod : plan.modEntries) {
                copied += copyMod(mod, outputDir);
            }
            writePackMcmeta(outputDir);
            ChatNotifier.sendDumpComplete(outputDir.getName(), copied);
        } catch (Exception e) {
            Log.warn("Dump copy failed: {}", e.toString());
            ChatNotifier.sendDumpFailed();
        } finally {
            busy.set(false);
        }
    }

    private static List<ModAssets> scanAllMods() {
        List<ModAssets> result = new ArrayList<>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            File source = container.getSource();
            if (source == null || !source.exists()) {
                continue;
            }
            ModAssets assets = source.isFile() ? scanJar(container.getModId(), source)
                    : scanDirectory(container.getModId(), source);
            if (assets != null && !assets.paths.isEmpty()) {
                result.add(assets);
            }
        }
        return result;
    }

    private static ModAssets scanJar(String modId, File source) {
        List<String> paths = new ArrayList<>();
        long totalBytes = 0L;
        try (ZipFile zip = new ZipFile(source)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.startsWith("assets/")) {
                    continue;
                }
                paths.add(name);
                totalBytes += Math.max(0L, entry.getSize());
            }
        } catch (IOException e) {
            Log.warn("Failed to scan mod jar {} ({}): {}", modId, source, e.toString());
            return null;
        }
        return new ModAssets(modId, source, true, paths, totalBytes);
    }

    private static ModAssets scanDirectory(String modId, File source) {
        File assetsDir = new File(source, "assets");
        if (!assetsDir.isDirectory()) {
            return new ModAssets(modId, source, false, new ArrayList<>(), 0L);
        }
        List<String> paths = new ArrayList<>();
        long[] totalBytes = { 0L };
        Path root = source.toPath();
        try (Stream<Path> walk = Files.walk(assetsDir.toPath())) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                try {
                    paths.add(root.relativize(p).toString().replace(File.separatorChar, '/'));
                    totalBytes[0] += Files.size(p);
                } catch (IOException e) {
                    Log.warn("Failed to stat {}: {}", p, e.toString());
                }
            });
        } catch (IOException e) {
            Log.warn("Failed to scan mod directory {} ({}): {}", modId, source, e.toString());
            return null;
        }
        return new ModAssets(modId, source, false, paths, totalBytes[0]);
    }

    private static int copyMod(ModAssets mod, File outputDir) {
        if (mod.isJar) {
            return copyFromJar(mod, outputDir);
        }
        return copyFromDirectory(mod, outputDir);
    }

    private static int copyFromJar(ModAssets mod, File outputDir) {
        int copied = 0;
        try (ZipFile zip = new ZipFile(mod.source)) {
            for (String path : mod.paths) {
                ZipEntry entry = zip.getEntry(path);
                if (entry == null) {
                    continue;
                }
                File dest = new File(outputDir, path);
                dest.getParentFile().mkdirs();
                try (InputStream in = zip.getInputStream(entry);
                        OutputStream out = Files.newOutputStream(dest.toPath())) {
                    copyStream(in, out);
                    copied++;
                } catch (IOException e) {
                    Log.warn("Failed to copy {} from {}: {}", path, mod.source, e.toString());
                }
            }
        } catch (IOException e) {
            Log.warn("Failed to reopen mod jar {}: {}", mod.source, e.toString());
        }
        return copied;
    }

    private static int copyFromDirectory(ModAssets mod, File outputDir) {
        int copied = 0;
        for (String path : mod.paths) {
            File src = new File(mod.source, path);
            File dest = new File(outputDir, path);
            dest.getParentFile().mkdirs();
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                Log.warn("Failed to copy {}: {}", src, e.toString());
            }
        }
        return copied;
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private static void writePackMcmeta(File outputDir) throws IOException {
        outputDir.mkdirs();
        String json = "{\n" + "  \"pack\": {\n"
                + "    \"pack_format\": 1,\n"
                + "    \"description\": \"Dumped assets from all loaded mods\"\n"
                + "  }\n"
                + "}\n";
        Files.write(
                new File(outputDir, "pack.mcmeta").toPath(),
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static final class ModAssets {

        final String modId;
        final File source;
        final boolean isJar;
        final List<String> paths;
        final long totalBytes;

        ModAssets(String modId, File source, boolean isJar, List<String> paths, long totalBytes) {
            this.modId = modId;
            this.source = source;
            this.isJar = isJar;
            this.paths = paths;
            this.totalBytes = totalBytes;
        }
    }

    private static final class PendingDump {

        final List<ModAssets> modEntries;
        final long createdAtMillis;

        PendingDump(List<ModAssets> modEntries, long createdAtMillis) {
            this.modEntries = modEntries;
            this.createdAtMillis = createdAtMillis;
        }
    }
}
