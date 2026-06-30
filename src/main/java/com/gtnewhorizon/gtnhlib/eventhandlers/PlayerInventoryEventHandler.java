package com.gtnewhorizon.gtnhlib.eventhandlers;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;

import com.gtnewhorizon.gtnhlib.client.PlayerInventoryClientHelper;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.inventory.InventoryEventListeners;
import com.gtnewhorizon.gtnhlib.inventory.InventoryEventPoster;
import com.gtnewhorizon.gtnhlib.inventory.PlayerInvState;
import com.gtnewhorizon.gtnhlib.inventory.PlayerInventoryScanner;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

@EventBusSubscriber
public final class PlayerInventoryEventHandler {

    private static final Object2ObjectOpenHashMap<UUID, PlayerInvState> SERVER_STATES = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<UUID, PlayerInvState> CLIENT_STATES = new Object2ObjectOpenHashMap<>();
    private static final InventoryEventPoster SERVER_POSTER = new InventoryEventPoster();
    private static final InventoryEventPoster CLIENT_POSTER = new InventoryEventPoster();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        final EntityPlayer player = event.player;
        if (player == null || player.inventory == null || player.worldObj == null) return;

        final boolean client = player.worldObj.isRemote;
        // 'client' must be evaluated first; it short-circuits so a dedicated server never classloads Minecraft via the
        // helper.
        if (client && !PlayerInventoryClientHelper.isLocalPlayer(player)) return;

        final Object2ObjectOpenHashMap<UUID, PlayerInvState> states = client ? CLIENT_STATES : SERVER_STATES;

        // Skip all scanning when nothing listens. Drop baselines so we re-seed (no backlog burst) once a listener returns.
        if (!InventoryEventListeners.anySubscribed()) {
            if (!states.isEmpty()) states.clear();
            return;
        }

        final UUID id = player.getUniqueID();
        PlayerInvState state = states.get(id);
        if (state == null) {
            state = new PlayerInvState();
            states.put(id, state);
        }

        PlayerInventoryScanner.process(player, state, client ? CLIENT_POSTER : SERVER_POSTER);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (event.player != null) SERVER_STATES.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public static void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        CLIENT_STATES.clear();
    }
}
