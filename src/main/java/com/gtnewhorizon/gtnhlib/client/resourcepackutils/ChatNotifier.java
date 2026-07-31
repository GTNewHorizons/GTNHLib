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
        String header = StatCollector
                .translateToLocalFormatted("gtnhlib.chat.resourcepackutils.update_available", packName, remote, installed);
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
                    new ChatComponentText(StatCollector.translateToLocal("gtnhlib.chat.resourcepackutils.open_release")));
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(root);
        });
    }

    static void sendLineMismatch(String packName, String installedLine, String playerLine) {
        String message = StatCollector
                .translateToLocalFormatted("gtnhlib.chat.resourcepackutils.line_mismatch", packName, installedLine, playerLine);
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
}
