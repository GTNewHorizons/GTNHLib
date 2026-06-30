package com.gtnewhorizon.gtnhlib.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Fired when a player's inventory changes (both client and server). Not cancelable.
 * Subscribe to concrete {@link ItemAdded} / {@link ItemRemoved} subclasses (1.7.10 Forge cannot register against
 * abstract base without no-arg constructor).
 */
public abstract class InventoryChangedEvent extends PlayerEvent {

    /** The changed item (representative; identity ignores NBT). Shared across listeners - copy before mutating. */
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

    /** Player gained {@link #item}. */
    public static class ItemAdded extends InventoryChangedEvent {

        public ItemAdded(EntityPlayer player, ItemStack item) {
            super(player, item);
        }

        @Override
        public int getDelta() {
            return item.stackSize;
        }
    }

    /** Player lost {@link #item}. */
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
