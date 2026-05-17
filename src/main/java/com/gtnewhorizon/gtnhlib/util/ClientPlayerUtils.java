package com.gtnewhorizon.gtnhlib.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class ClientPlayerUtils {

    public static final Map<UUID, String> clientUsernameCache = new HashMap<>();
    public static final Map<String, UUID> clientPlayerMap = new HashMap<>();

    public static String getPlayerName(EntityPlayer player) {
        return player.getCommandSenderName();
    }

    public static String getPlayerName(UUID player) {
        synchronized (clientUsernameCache) {
            return clientUsernameCache.get(player);
        }
    }

    public static Set<String> getOnlinePlayers() {
        return Minecraft.getMinecraft().thePlayer.sendQueue.playerInfoList.stream().map(info -> info.name)
                .collect(Collectors.toSet());
    }

    public static UUID getPlayerUUID(String name) {
        synchronized (clientPlayerMap) {
            return clientPlayerMap.get(name);
        }
    }
}
