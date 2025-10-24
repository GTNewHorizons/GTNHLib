package com.gtnewhorizon.gtnhlib.capability.item;

import java.util.BitSet;
import java.util.OptionalInt;

import javax.annotation.Nullable;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.datastructs.extensions.IterableBitSet;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;

public class InventoryItemSink implements IItemSink {

    public final IInventory inv;
    private final ForgeDirection side;

    private boolean markedDirty = false;

    private BitSet allowedSlots = null;

    public InventoryItemSink(IInventory inv, ForgeDirection side) {
        this.inv = inv;
        this.side = side;
    }

    @Override
    public void setAllowedSinkSlots(int @org.jetbrains.annotations.Nullable [] slots) {
        if (slots == null) {
            allowedSlots = null;
            return;
        }

        if (allowedSlots == null) {
            allowedSlots = new BitSet();
        } else {
            allowedSlots.clear();
        }

        for (int slot : slots) {
            allowedSlots.set(slot);
        }
    }

    @Override
    public ItemStack store(ItemStack stack) {
        stack = ItemStack.copyItemStack(stack);

        if (stack.stackSize <= 0) return null;

        this.markedDirty = false;

        IntIterable slots;

        if (inv instanceof ISidedInventory sided) {
            slots = IntArrayList.wrap(sided.getAccessibleSlotsFromSide(side.ordinal()));
        } else {
            slots = () -> IntIterators.fromTo(0, inv.getSizeInventory());
        }

        IterableBitSet emptySlots = null;

        for (IntIterator iterator = slots.iterator(); iterator.hasNext();) {
            int slot = iterator.nextInt();

            if (allowedSlots != null && !allowedSlots.get(slot)) continue;

            ItemStack inSlot = inv.getStackInSlot(slot);

            if (ItemUtil.isStackEmpty(inSlot)) {
                if (emptySlots == null) emptySlots = new IterableBitSet();
                emptySlots.set(slot);
                continue;
            }

            if (!ItemUtil.areStacksEqual(inSlot, stack)) continue;

            if (!inv.isItemValidForSlot(slot, stack)) continue;

            if (side != ForgeDirection.UNKNOWN && inv instanceof ISidedInventory sided) {
                if (!sided.canInsertItem(slot, stack, side.ordinal())) continue;
            }

            int slotLimit = getSlotStackLimit(slot, inSlot);
            int remaining = slotLimit - inSlot.stackSize;

            int toTransfer = Math.min(remaining, stack.stackSize);

            stack.stackSize -= toTransfer;
            inSlot.stackSize += toTransfer;

            inv.setInventorySlotContents(slot, inSlot);

            markDirty();

            if (stack.stackSize <= 0) return null;
        }

        if (stack.stackSize <= 0) return null;
        if (emptySlots == null) return stack;

        for (int slot : emptySlots) {
            if (!inv.isItemValidForSlot(slot, stack)) continue;

            if (side != ForgeDirection.UNKNOWN && inv instanceof ISidedInventory sided) {
                if (!sided.canInsertItem(slot, stack, side.ordinal())) continue;
            }

            int slotStackLimit = getSlotStackLimit(slot, stack);
            int toTransfer = Math.min(slotStackLimit, stack.stackSize);

            ItemStack inserted = ItemUtil.copyAmount(toTransfer, stack);
            inv.setInventorySlotContents(slot, inserted);
            stack.stackSize -= toTransfer;

            markDirty();

            if (stack.stackSize <= 0) return null;
        }

        return stack.stackSize <= 0 ? null : stack;
    }

    private void markDirty() {
        if (markedDirty) return;

        inv.markDirty();
        markedDirty = true;
    }

    @Override
    public OptionalInt getStoredAmount(@Nullable ItemStack stack) {
        long sum = 0;

        int len = inv.getSizeInventory();

        for (int i = 0; i < len; i++) {
            ItemStack inSlot = inv.getStackInSlot(i);

            if (ItemUtil.isStackEmpty(inSlot)) continue;

            if (stack == null || ItemUtil.areStacksEqual(inSlot, stack)) {
                sum += inSlot.stackSize;
            }
        }

        return OptionalInt.of(longToInt(sum));
    }

    private static int longToInt(long number) {
        return (int) Math.min(Integer.MAX_VALUE, number);
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
