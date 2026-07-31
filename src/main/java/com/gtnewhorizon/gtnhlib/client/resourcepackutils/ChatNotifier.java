package com.gtnewhorizon.gtnhlib.client.resourcepackutils;

import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

final class ChatNotifier {

    private ChatNotifier() {}

    static void sendUpdateMessage(String packName, String installed, String remote, String url) {
        String header = StatCollector.translateToLocalFormatted(
                "gtnhlib.chat.resourcepackutils.update_available",
                packName,
                remote,
                installed);
        UpdateEventHandler.enqueue(() -> {
            IChatComponent headerComponent = new ChatComponentText(header);
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(headerComponent);

            IChatComponent root = new ChatComponentText("");
            ChatComponentText clickable = new ChatComponentText(
                    StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.click_here"));
            ChatStyle style = new ChatStyle();
            style.setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            clickable.setChatStyle(style);
            root.appendSibling(clickable);
            root.appendSibling(
                    new ChatComponentText(
                            StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.open_release")));
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(root);
        });
    }

    static void sendLineMismatch(String packName, String installedLine, String playerLine) {
        String message = StatCollector.translateToLocalFormatted(
                "gtnhlib.chat.resourcepackutils.line_mismatch",
                packName,
                installedLine,
                playerLine);
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendNoUpdatesFound() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.no_updates");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendCooldownMessage() {
        String command = "/resourcepack updateCheck force";
        String message = StatCollector.translateToLocalFormatted("gtnhlib.chat.resourcepackutils.cooldown", command);
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendChecking() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.checking");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendAlreadyRunning() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.already_running");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendManualCooldown() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.manual_cooldown");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendDumpScanning() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.dump.scanning");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendDumpSummary(int modCount, int fileCount, long totalBytes) {
        String size = formatBytes(totalBytes);
        String message = StatCollector
                .translateToLocalFormatted("gtnhlib.chat.resourcepackutils.dump.summary", modCount, fileCount, size);
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendDumpScanFailed() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.dump.scan_failed");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendDumpAlreadyRunning() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.dump.already_running");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendDumpNoPending() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.dump.no_pending");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendDumpExpired() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.dump.expired");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendDumpComplete(String folderName, int fileCount) {
        String message = StatCollector
                .translateToLocalFormatted("gtnhlib.chat.resourcepackutils.dump.complete", fileCount, folderName);
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendDumpFailed() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.dump.failed");
        UpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024.0) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024.0) {
            return String.format("%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
}
