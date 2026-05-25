package com.gtnewhorizon.gtnhlib.util;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;

public class ClientPlayerUtils {

    public static final Set<String> clientUsernameCache = new HashSet<>();

    public static String getPlayerName(EntityPlayer player) {
        return player.getCommandSenderName();
    }

    public static Set<String> getOnlinePlayers() {
        return Minecraft.getMinecraft().thePlayer.sendQueue.playerInfoList.stream().map(info -> info.name)
                .collect(Collectors.toSet());
    }

    public static String getCurrentPlayerName() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player != null) return player.getCommandSenderName();

        return Minecraft.getMinecraft().getSession().getUsername();
    }
}
