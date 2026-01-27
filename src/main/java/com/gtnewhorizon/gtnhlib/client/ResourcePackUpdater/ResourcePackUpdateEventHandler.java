package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public final class ResourcePackUpdateEventHandler {

    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();
    private static final int AUTO_RUN_DELAY_TICKS = 100; // 5 seconds

    private boolean pendingAutoRun = false;
    private int autoRunCountdown = 0;

    static void enqueue(Runnable task) {
        if (task != null) {
            TASKS.add(task);
        }
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        pendingAutoRun = true;
        autoRunCountdown = AUTO_RUN_DELAY_TICKS;
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
        if (autoRunCountdown > 0) {
            autoRunCountdown--;
            return;
        }
        pendingAutoRun = false;
        ResourcePackUpdateChecker.runAutoCheckIfNeeded();
    }

    @SubscribeEvent
    public void onClientTickQueue(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Runnable task;
        while ((task = TASKS.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                RpUpdaterLog.warn("Task execution failed: {}", e.toString());
            }
        }
    }
}
