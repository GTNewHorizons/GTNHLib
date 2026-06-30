package com.gtnewhorizon.gtnhlib.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.compat.BaublesCompat;
import com.gtnewhorizon.gtnhlib.compat.Mods;

import it.unimi.dsi.fastutil.longs.Long2IntMap;

/** Builds the per-tick inventory snapshot and diffs it. */
public final class PlayerInventoryScanner {

    private PlayerInventoryScanner() {}

    /** @param poster a reusable per-side consumer; its {@code player} is set here before diffing. */
    public static void process(EntityPlayer player, PlayerInvState state, InventoryEventPoster poster) {
        if (++state.ticksSinceScan < GTNHLibConfig.inventoryScanInterval) return;
        state.ticksSinceScan = 0;

        final Long2IntMap current = state.current;
        current.clear();
        scan(player, current);

        if (!state.seeded) {
            state.seeded = true;
            state.swap();
            return;
        }

        poster.player = player;
        InventoryDiffer.diff(state.previous, current, poster);
        state.swap();
    }

    private static void scan(EntityPlayer player, Long2IntMap out) {
        accumulateArray(player.inventory.mainInventory, out);
        accumulateArray(player.inventory.armorInventory, out);
        accumulate(player.inventory.getItemStack(), out); // cursor / held stack
        if (player.inventoryContainer instanceof ContainerPlayer) {
            accumulateInventory(((ContainerPlayer) player.inventoryContainer).craftMatrix, out);
        }
        if (Mods.BAUBLES) {
            accumulateInventory(BaublesCompat.getBaubles(player), out);
        }
    }

    private static void accumulateArray(ItemStack[] stacks, Long2IntMap out) {
        if (stacks == null) return;
        for (ItemStack s : stacks) accumulate(s, out);
    }

    private static void accumulateInventory(IInventory inv, Long2IntMap out) {
        if (inv == null) return;
        final int size = inv.getSizeInventory();
        for (int i = 0; i < size; i++) accumulate(inv.getStackInSlot(i), out);
    }

    private static void accumulate(ItemStack stack, Long2IntMap out) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return;
        final Item item = stack.getItem();
        final long key = ItemIdentity.pack(Item.getIdFromItem(item), stack.getItemDamage(), item.getHasSubtypes());
        out.put(key, out.get(key) + stack.stackSize);
    }
}
