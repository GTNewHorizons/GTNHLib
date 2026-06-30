package com.gtnewhorizon.gtnhlib.inventory;

import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.event.InventoryChangedEvent;
import com.gtnewhorizon.gtnhlib.mixins.early.fml.EventBusAccessor;

import cpw.mods.fml.common.eventhandler.ListenerList;

/**
 * Cheap "is anyone listening" check so the per-player scan can be skipped entirely when no mod subscribes to the
 * inventory change events.
 */
public final class InventoryEventListeners {

    private static ListenerList added;
    private static ListenerList removed;
    private static int forgeBusId;
    private static volatile boolean ready;

    private InventoryEventListeners() {}

    public static boolean anySubscribed() {
        if (!ready) init();
        return added.getListeners(forgeBusId).length > 0 || removed.getListeners(forgeBusId).length > 0;
    }

    private static synchronized void init() {
        if (ready) return;
        forgeBusId = ((EventBusAccessor) (Object) MinecraftForge.EVENT_BUS).getBusID();
        // The throwaway instances exist only to reach the per-class ListenerList; their fields are never read.
        added = new InventoryChangedEvent.ItemAdded(null, null).getListenerList();
        removed = new InventoryChangedEvent.ItemRemoved(null, null).getListenerList();
        ready = true;
    }
}
