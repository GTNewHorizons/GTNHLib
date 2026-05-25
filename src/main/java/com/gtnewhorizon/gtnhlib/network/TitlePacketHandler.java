package com.gtnewhorizon.gtnhlib.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

public class TitlePacketHandler {

    public static void sendTitle(EntityPlayerMP player, IChatComponent component) {
        NetworkHandler.instance.sendTo(MessageTitle.title(component), player);
    }

    public static void sendSubtitle(EntityPlayerMP player, IChatComponent component) {
        NetworkHandler.instance.sendTo(MessageTitle.subtitle(component), player);
    }

    public static void sendTimes(EntityPlayerMP player, int fadeIn, int stay, int fadeOut) {
        NetworkHandler.instance.sendTo(MessageTitle.times(fadeIn, stay, fadeOut), player);
    }

    public static void sendClear(EntityPlayerMP player) {
        NetworkHandler.instance.sendTo(MessageTitle.clear(), player);
    }

    public static void sendReset(EntityPlayerMP player) {
        NetworkHandler.instance.sendTo(MessageTitle.reset(), player);
    }

    public static void sendIcon(EntityPlayerMP player, ItemStack stack) {
        NetworkHandler.instance.sendTo(MessageTitle.icon(stack), player);
    }
}
