package com.gtnewhorizon.gtnhlib.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.event.InventoryChangedEvent;

/**
 * Reusable {@link InventoryDiffer.DeltaConsumer} that turns a packed-key delta into a posted event. One instance per
 * logical side, reused across scans; {@link #player} is assigned before each diff so no per-scan lambda is allocated.
 */
public final class InventoryEventPoster implements InventoryDiffer.DeltaConsumer {

    public EntityPlayer player;

    @Override
    public void accept(long key, int delta) {
        if (delta == 0) return;
        final Item item = Item.getItemById(ItemIdentity.unpackId(key));
        if (item == null) return;
        final int meta = ItemIdentity.unpackMeta(key);
        if (delta > 0) {
            MinecraftForge.EVENT_BUS.post(new InventoryChangedEvent.ItemAdded(player, new ItemStack(item, delta, meta)));
        } else {
            MinecraftForge.EVENT_BUS.post(new InventoryChangedEvent.ItemRemoved(player, new ItemStack(item, -delta, meta)));
        }
    }
}
