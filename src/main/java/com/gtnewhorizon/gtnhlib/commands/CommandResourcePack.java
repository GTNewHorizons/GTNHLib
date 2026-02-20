package com.gtnewhorizon.gtnhlib.commands;

import java.util.List;
import java.util.Locale;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater.ResourcePackUpdateChecker;

public class CommandResourcePack extends GTNHClientCommand {

    @Override
    public String getCommandName() {
        return "resourcepack";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/resourcepack updateCheck [force]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            printHelp();
            return;
        }
        if ("updateCheck".equalsIgnoreCase(args[0])) {
            boolean force = args.length > 1 && "force".equalsIgnoreCase(args[1]);
            if (!GTNHLibConfig.enableResourcePackUpdateCheck && !force) {
                addChatMessage(StatCollector.translateToLocal("gtnhlib.chat.rpupdater.disabled"));
                return;
            }
            ResourcePackUpdateChecker.runManualCheck(force);
            return;
        }
        if ("list".equalsIgnoreCase(args[0])) {
            printList();
            return;
        }
        if ("status".equalsIgnoreCase(args[0])) {
            printStatus();
            return;
        }
        if ("info".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                addChatMessage(
                        StatCollector.translateToLocalFormatted(
                                "gtnhlib.chat.rpupdater.info.usage",
                                "/resourcepack info <packName>"));
                return;
            }
            printInfo(args[1]);
            return;
        }
        printHelp();
    }

    private void printHelp() {
        addChatMessage(StatCollector.translateToLocal("gtnhlib.chat.rpupdater.commands"));
        addChatMessage("/resourcepack updateCheck [force]");
        addChatMessage("/resourcepack list");
        addChatMessage("/resourcepack status");
        addChatMessage("/resourcepack info <packName>");
        addChatMessage("/resourcepack help");
    }

    private void printList() {
        List<ResourcePackUpdateChecker.PackSummary> packs = ResourcePackUpdateChecker.getActivePackSummaries();
        addChatMessage(StatCollector.translateToLocalFormatted("gtnhlib.chat.rpupdater.list.header", packs.size()));
        for (ResourcePackUpdateChecker.PackSummary pack : packs) {
            if (pack.hasUpdater) {
                addChatMessage(
                        StatCollector.translateToLocalFormatted(
                                "gtnhlib.chat.rpupdater.list.item.updater",
                                pack.packDisplayName,
                                pack.updaterPackName,
                                pack.packVersion,
                                pack.packGameVersion));
            } else {
                addChatMessage(
                        StatCollector.translateToLocalFormatted(
                                "gtnhlib.chat.rpupdater.list.item.none",
                                pack.packDisplayName));
            }
        }
    }

    private void printStatus() {
        ResourcePackUpdateChecker.StatusSnapshot status = ResourcePackUpdateChecker.getStatusSnapshot();
        addChatMessage(StatCollector.translateToLocal("gtnhlib.chat.rpupdater.status.header"));
        addChatMessage(
                StatCollector.translateToLocalFormatted(
                        "gtnhlib.chat.rpupdater.status.running",
                        status.running ? "yes" : "no"));
        String lastCheck = status.lastCheckMillis == 0L
                ? StatCollector.translateToLocal("gtnhlib.chat.rpupdater.status.never")
                : formatSecondsAgo(status.lastCheckMillis);
        addChatMessage(StatCollector.translateToLocalFormatted("gtnhlib.chat.rpupdater.status.last_check", lastCheck));
        if (status.failureCooldownRemainingMillis > 0L) {
            addChatMessage(
                    StatCollector.translateToLocalFormatted(
                            "gtnhlib.chat.rpupdater.status.cooldown_failure",
                            formatSecondsRemaining(status.failureCooldownRemainingMillis)));
        }
        if (status.manualCooldownRemainingMillis > 0L) {
            addChatMessage(
                    StatCollector.translateToLocalFormatted(
                            "gtnhlib.chat.rpupdater.status.cooldown_manual",
                            formatSecondsRemaining(status.manualCooldownRemainingMillis)));
        }
    }

    private void printInfo(String query) {
        String needle = query.toLowerCase(Locale.ENGLISH);
        List<ResourcePackUpdateChecker.PackSummary> packs = ResourcePackUpdateChecker.getActivePackSummaries();
        List<ResourcePackUpdateChecker.PackSummary> matches = new java.util.ArrayList<>();
        for (ResourcePackUpdateChecker.PackSummary pack : packs) {
            String display = pack.packDisplayName.toLowerCase(Locale.ENGLISH);
            String updater = pack.updaterPackName == null ? "" : pack.updaterPackName.toLowerCase(Locale.ENGLISH);
            if (display.contains(needle) || updater.contains(needle)) {
                matches.add(pack);
            }
        }
        if (matches.isEmpty()) {
            addChatMessage(StatCollector.translateToLocalFormatted("gtnhlib.chat.rpupdater.info.not_found", query));
            return;
        }
        if (matches.size() > 1) {
            addChatMessage(
                    StatCollector
                            .translateToLocalFormatted("gtnhlib.chat.rpupdater.info.ambiguous", query, matches.size()));
            return;
        }
        ResourcePackUpdateChecker.PackSummary pack = matches.get(0);
        addChatMessage(
                StatCollector.translateToLocalFormatted("gtnhlib.chat.rpupdater.info.header", pack.packDisplayName));
        if (!pack.hasUpdater) {
            addChatMessage(StatCollector.translateToLocal("gtnhlib.chat.rpupdater.info.no_updater"));
            return;
        }
        addChatMessage(
                StatCollector
                        .translateToLocalFormatted("gtnhlib.chat.rpupdater.info.updater_name", pack.updaterPackName));
        addChatMessage(
                StatCollector.translateToLocalFormatted(
                        "gtnhlib.chat.rpupdater.info.version",
                        pack.packVersion,
                        pack.packGameVersion));
        addChatMessage(
                StatCollector.translateToLocalFormatted("gtnhlib.chat.rpupdater.info.source", pack.owner, pack.repo));
    }

    private static String formatSecondsRemaining(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return seconds + "s";
    }

    private static String formatSecondsAgo(long millis) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        return seconds + "s";
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "updateCheck", "list", "status", "info", "help");
        }
        if (args.length == 2 && "updateCheck".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "force");
        }
        return null;
    }
}
