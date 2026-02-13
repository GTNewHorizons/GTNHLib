package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

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
                .translateToLocalFormatted("gtnhlib.chat.rpupdater.update_available", packName, remote, installed);
        ResourcePackUpdateEventHandler.enqueue(() -> {
            IChatComponent headerComponent = new ChatComponentText(header);
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(headerComponent);

            IChatComponent root = new ChatComponentText("");
            ChatComponentText clickable = new ChatComponentText(
                    StatCollector.translateToLocal("gtnhlib.chat.rpupdater.click_here"));
            ChatStyle style = new ChatStyle();
            style.setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            clickable.setChatStyle(style);
            root.appendSibling(clickable);
            root.appendSibling(
                    new ChatComponentText(StatCollector.translateToLocal("gtnhlib.chat.rpupdater.open_release")));
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(root);
        });
    }

    static void sendLineMismatch(String packName, String installedLine, String playerLine) {
        String message = StatCollector
                .translateToLocalFormatted("gtnhlib.chat.rpupdater.line_mismatch", packName, installedLine, playerLine);
        ResourcePackUpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendNoUpdatesFound() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.rpupdater.no_updates");
        ResourcePackUpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendCooldownMessage() {
        String command = "/resourcepack updateCheck force";
        String message = StatCollector.translateToLocalFormatted("gtnhlib.chat.rpupdater.cooldown", command);
        ResourcePackUpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendChecking() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.rpupdater.checking");
        ResourcePackUpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendAlreadyRunning() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.rpupdater.already_running");
        ResourcePackUpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }

    static void sendManualCooldown() {
        String message = StatCollector.translateToLocal("gtnhlib.chat.rpupdater.manual_cooldown");
        ResourcePackUpdateEventHandler.enqueue(
                () -> Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message)));
    }
}
