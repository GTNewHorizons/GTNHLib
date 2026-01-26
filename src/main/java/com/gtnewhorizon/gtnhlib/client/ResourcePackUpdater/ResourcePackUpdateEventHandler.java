package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public final class ResourcePackUpdateEventHandler {

    private boolean pendingAutoRun = false;

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        pendingAutoRun = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!pendingAutoRun) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        pendingAutoRun = false;
        ResourcePackUpdateChecker.runAutoCheckIfNeeded();
    }
}
