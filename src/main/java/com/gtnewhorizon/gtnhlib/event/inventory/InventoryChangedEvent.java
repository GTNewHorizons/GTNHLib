package com.gtnewhorizon.gtnhlib.event.inventory;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.common.eventhandler.Event;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Base event for centralized inventory scanner changes.
 * <p>
 * Requires an explicit call to {@link InventoryChangeScanner#requireScanner()} by at least one consumer mod.
 * <p>
 * This event is posted on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS}.
 */
public abstract class InventoryChangedEvent extends Event {

    private final EntityPlayer player;
    private final Object2IntMap<InventoryKey> changes;

    protected InventoryChangedEvent(EntityPlayer player, Object2IntMap<InventoryKey> changes) {
        this.player = player;
        // Defensive copy so listeners never observe scanner map reuse/mutation across ticks.
        this.changes = Object2IntMaps.unmodifiable(new Object2IntOpenHashMap<>(changes));
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    /**
     * @return Immutable map of changed inventory keys to their count delta for this direction.
     */
    public Object2IntMap<InventoryKey> getChanges() {
        return changes;
    }

    /**
     * Posted when items have entered player-owned inventory scope.
     */
    public static final class Entered extends InventoryChangedEvent {

        public Entered(EntityPlayer player, Object2IntMap<InventoryKey> entered) {
            super(player, entered);
        }
    }

    /**
     * Posted when items have left player-owned inventory scope.
     */
    public static final class Left extends InventoryChangedEvent {

        public Left(EntityPlayer player, Object2IntMap<InventoryKey> left) {
            super(player, left);
        }
    }
}
