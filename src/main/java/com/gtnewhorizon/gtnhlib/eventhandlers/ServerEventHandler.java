package com.gtnewhorizon.gtnhlib.eventhandlers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraftforge.common.UsernameCache;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.PlayerDataSync;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

@EventBusSubscriber
public class ServerEventHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Map<String, UUID> newMap = new HashMap<>();
        UsernameCache.getMap().forEach((uuid, name) -> { newMap.put(name, uuid); });
        synchronized (ServerPlayerUtils.serverPlayerMap) {
            ServerPlayerUtils.serverPlayerMap.clear();
            ServerPlayerUtils.serverPlayerMap.putAll(newMap);
        }
        PlayerDataSync sync = new PlayerDataSync();
        sync.data.putAll(newMap);
        ServerPlayerUtils.forAllOnlinePlayers(player -> NetworkHandler.instance.sendTo(sync, player));
    }
}
