package com.gtnewhorizon.gtnhlib.config;

import static com.gtnewhorizon.gtnhlib.config.ConfigurationManager.LOGGER;

import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;

import com.gtnewhorizon.gtnhlib.network.NetworkHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

@SuppressWarnings("unused")
public final class ConfigSyncHandler {

    static final Map<String, SyncedConfigElement> syncedElements = new Object2ObjectOpenHashMap<>();
    private static boolean hasSyncedValues = false;

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP playerMP)) return;
        MinecraftServer server = MinecraftServer.getServer();
        // no point in syncing in from client -> client.
        if (server.isSinglePlayer() && !((IntegratedServer) server).getPublic()) {
            return;
        }
        NetworkHandler.instance.sendTo(new PacketSyncConfig(syncedElements.values()), playerMP);
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (!hasSyncedValues) return;
        hasSyncedValues = false;
        for (SyncedConfigElement element : syncedElements.values()) {
            element.restoreValue();
        }
    }

    static void onSync(PacketSyncConfig packet) {
        for (Object2ObjectMap.Entry<String, String> entry : packet.syncedElements.object2ObjectEntrySet()) {
            SyncedConfigElement element = syncedElements.get(entry.getKey());
            if (element != null) {
                try {
                    hasSyncedValues = true;
                    element.setSyncValue(entry.getValue());
                } catch (ConfigException e) {
                    LOGGER.error("Failed to sync element {}", element, e);
                }
            }
        }
    }

    static boolean hasSyncedValues() {
        return hasSyncedValues;
    }
}
