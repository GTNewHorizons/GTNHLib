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
        IChatComponent root = new ChatComponentText(header + " ");
        ChatComponentText clickable = new ChatComponentText(
                StatCollector.translateToLocal("gtnhlib.chat.rpupdater.click_here"));
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        clickable.setChatStyle(style);
        root.appendSibling(clickable);
        root.appendSibling(
                new ChatComponentText(StatCollector.translateToLocal("gtnhlib.chat.rpupdater.open_release")));
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(root);
    }

    static void sendLineMismatch(String packName, String installedLine, String playerLine) {
        String message = StatCollector
                .translateToLocalFormatted("gtnhlib.chat.rpupdater.line_mismatch", packName, installedLine, playerLine);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message));
    }
}
