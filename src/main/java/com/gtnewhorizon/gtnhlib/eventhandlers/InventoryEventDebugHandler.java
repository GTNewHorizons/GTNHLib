package com.gtnewhorizon.gtnhlib.eventhandlers;

import net.minecraft.entity.player.EntityPlayer;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.event.InventoryChangedEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.ChatComponentText;

/** Dev-only listener that logs inventory change events. Registered only in deobf with debugInventoryEvents=true. */
public final class InventoryEventDebugHandler {

    @SubscribeEvent
    public void onItemAdded(InventoryChangedEvent.ItemAdded event) {
        log(event);
    }

    @SubscribeEvent
    public void onItemRemoved(InventoryChangedEvent.ItemRemoved event) {
        log(event);
    }

    private static void log(InventoryChangedEvent event) {
        final EntityPlayer player = event.entityPlayer;
        final String side = player.worldObj.isRemote ? "CLIENT" : "SERVER";
        final String verb = event.getDelta() > 0 ? "ADDED" : "REMOVED";
        GTNHLib.LOG.info(
                "[InvEvent/{}] {} (delta {}) {} x{} for {}",
                side,
                verb,
                event.getDelta(),
                event.item.getDisplayName(),
                event.getCount(),
                player.getCommandSenderName());
        // Chat only on the client so single-player does not double up (the server event fires the same change).
        if (player.worldObj.isRemote) {
            player.addChatMessage(
                    new ChatComponentText("[InvEvent] " + verb + " " + event.item.getDisplayName() + " x"
                            + event.getCount()));
        }
    }
}
