package com.gtnewhorizon.gtnhlib.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Base for events fired on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS} when a player's net holdings of
 * an item change. Fired on both client (local player only) and server. Not cancelable; the change already happened.
 * <p>
 * {@link #item} is a synthesized representative stack: its item and metadata identify what changed and its
 * {@code stackSize} is the absolute amount of the change. NBT is not preserved (identity ignores NBT).
 * <p>
 * Subscribe to the concrete {@link ItemAdded} / {@link ItemRemoved} subclasses; in 1.7.10 Forge cannot register a
 * listener against this abstract base (it has no no-arg constructor to instantiate). This type unifies the API for
 * shared/polymorphic handling.
 */
public abstract class InventoryChangedEvent extends PlayerEvent {

    public final ItemStack item;

    protected InventoryChangedEvent(EntityPlayer player, ItemStack item) {
        super(player);
        this.item = item;
    }

    /** Absolute amount of the change (always positive). */
    public int getCount() {
        return item.stackSize;
    }

    /** Signed change: positive when added, negative when removed. */
    public abstract int getDelta();

    /** A player's net holdings of {@link #item} increased by {@link #getCount()}. */
    public static class ItemAdded extends InventoryChangedEvent {

        public ItemAdded(EntityPlayer player, ItemStack item) {
            super(player, item);
        }

        @Override
        public int getDelta() {
            return item.stackSize;
        }
    }

    /** A player's net holdings of {@link #item} decreased by {@link #getCount()}. */
    public static class ItemRemoved extends InventoryChangedEvent {

        public ItemRemoved(EntityPlayer player, ItemStack item) {
            super(player, item);
        }

        @Override
        public int getDelta() {
            return -item.stackSize;
        }
    }
}
