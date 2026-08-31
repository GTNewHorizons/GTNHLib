package com.gtnewhorizon.gtnhlib.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.event.InventoryChangedEvent;

import it.unimi.dsi.fastutil.longs.Long2IntMap;

/**
 * Reusable {@link InventoryDiffer.DeltaConsumer} that turns a packed-key delta into a posted event.
 */
public final class InventoryEventPoster implements InventoryDiffer.DeltaConsumer {

    public EntityPlayer player;
    /** Current snapshot; supplies the post-change total for each identity. */
    public Long2IntMap current;

    @Override
    public void accept(long key, int delta) {
        if (delta == 0) return;
        final Item item = Item.getItemById(ItemIdentity.unpackId(key));
        if (item == null) return;
        final int meta = ItemIdentity.unpackMeta(key);
        final int total = current.get(key);
        if (delta > 0) {
            MinecraftForge.EVENT_BUS
                    .post(new InventoryChangedEvent.ItemAdded(player, new ItemStack(item, delta, meta), total));
        } else {
            MinecraftForge.EVENT_BUS
                    .post(new InventoryChangedEvent.ItemRemoved(player, new ItemStack(item, -delta, meta), total));
        }
    }
}
