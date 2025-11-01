package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import it.unimi.dsi.fastutil.ints.IntIterators;

public class InventoryItemSink implements ItemSink {

    public final IInventory inv;
    private final ForgeDirection side;

    private final int[] slots;

    private int[] allowedSlots;

    public InventoryItemSink(IInventory inv, ForgeDirection side) {
        this.inv = inv;
        this.side = side;

        if (inv instanceof ISidedInventory sided) {
            this.slots = sided.getAccessibleSlotsFromSide(side.ordinal());
        } else {
            this.slots = IntIterators.unwrap(IntIterators.fromTo(0, inv.getSizeInventory()));
        }
    }

    @Override
    public void resetSink() {
        ItemSink.super.resetSink();
        allowedSlots = null;
    }

    @Override
    public void setAllowedSinkSlots(int @Nullable [] slots) {
        this.allowedSlots = slots;
    }

    @Override
    public ItemStack store(ItemStack stack) {
        if (ItemUtil.isStackEmpty(stack)) return null;

        stack = stack.copy();

        StandardInventoryIterator iter = sinkIterator();

        while (iter.hasNext()) {
            ImmutableItemStack slot = iter.next();

            if (slot == null || slot.isEmpty()) continue;

            if (slot.matches(stack)) {
                int slotLimit = getSlotStackLimit(iter.getCurrentSlot(), stack);
                int remaining = slotLimit - slot.getStackSize();

                int toTransfer = Math.min(remaining, stack.stackSize);

                ItemStack rejected = iter.insert(stack.splitStack(toTransfer));

                if (rejected != null) {
                    stack.stackSize += rejected.stackSize;
                }

                if (ItemUtil.isStackEmpty(stack)) return null;
            }
        }

        iter.rewind();

        while (iter.hasNext()) {
            ImmutableItemStack slot = iter.next();

            if (slot == null || slot.isEmpty()) {
                int slotLimit = getSlotStackLimit(iter.getCurrentSlot(), stack);

                int toTransfer = Math.min(slotLimit, stack.stackSize);

                ItemStack rejected = iter.insert(stack.splitStack(toTransfer));

                if (rejected != null) {
                    stack.stackSize += rejected.stackSize;
                }

                if (ItemUtil.isStackEmpty(stack)) return null;
            }
        }

        return stack;
    }

    @Override
    public @NotNull StandardInventoryIterator sinkIterator() {
        return new StandardInventoryIterator(inv, side, slots, allowedSlots) {

            @Override
            protected int getSlotStackLimit(int slot, ItemStack stack) {
                return InventoryItemSink.this.getSlotStackLimit(slot, stack);
            }
        };
    }

    protected int getSlotStackLimit(int slot, ItemStack stack) {
        int invStackLimit = inv.getInventoryStackLimit();

        int existingMaxStack = stack == null ? 64 : stack.getMaxStackSize();

        if (invStackLimit > 64) {
            return invStackLimit / 64 * existingMaxStack;
        } else {
            return Math.min(invStackLimit, existingMaxStack);
        }
    }
}
