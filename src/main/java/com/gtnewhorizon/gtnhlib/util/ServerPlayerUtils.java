package com.gtnewhorizon.gtnhlib.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.UsernameCache;

public class ServerPlayerUtils {

    public static final Map<String, UUID> serverPlayerMap = new HashMap<>();

    public static String getPlayerName(EntityPlayer player) {
        return player.getCommandSenderName();
    }

    public static String getPlayerName(UUID player) {
        return UsernameCache.getLastKnownUsername(player);
    }

    public static EntityPlayer getPlayerByUUID(World world, UUID playerId) {
        return world.func_152378_a(playerId);
    }

    public static void forAllOnlinePlayers(Consumer<EntityPlayerMP> consumer) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;
        for (EntityPlayerMP playerEntity : server.getConfigurationManager().playerEntityList) {
            consumer.accept(playerEntity);
        }
    }

    public static Map<UUID, EntityPlayerMP> getOnlinePlayers() {
        Map<UUID, EntityPlayerMP> playerMap = new HashMap<>();
        forAllOnlinePlayers(player -> playerMap.put(player.getUniqueID(), player));
        return playerMap;
    }

    public static UUID getPlayerUUID(String name) {
        synchronized (serverPlayerMap) {
            return serverPlayerMap.get(name);
        }
    }
}
