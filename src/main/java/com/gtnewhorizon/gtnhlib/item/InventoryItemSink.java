package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;

import it.unimi.dsi.fastutil.ints.IntIterators;

public class InventoryItemSink implements ItemSink {

    public final IInventory inv;
    private final ForgeDirection side;

    private int[] allowedSlots;

    public InventoryItemSink(IInventory inv, ForgeDirection side) {
        this.inv = inv;
        this.side = side;
    }

    protected int[] getSlots() {
        if (this.inv instanceof ISidedInventory sided) {
            return sided.getAccessibleSlotsFromSide(this.side.ordinal());
        } else {
            return IntIterators.unwrap(IntIterators.fromTo(0, this.inv.getSizeInventory()));
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
    public int store(ImmutableItemStack stack) {
        if (stack.isEmpty()) return 0;

        StandardInventoryIterator iter = sinkIterator();

        InsertionItemStack insertion = new InsertionItemStack(stack);

        while (iter.hasNext()) {
            ImmutableItemStack slot = iter.next();

            if (slot == null || slot.isEmpty()) continue;

            insertion.set(iter.insert(insertion, false));

            if (insertion.isEmpty()) return 0;
        }

        iter.rewind();

        while (iter.hasNext()) {
            ImmutableItemStack slot = iter.next();

            if (slot != null && !slot.isEmpty()) continue;

            insertion.set(iter.insert(insertion, false));

            if (insertion.isEmpty()) return 0;
        }

        return insertion.getStackSize();
    }

    @Override
    public @NotNull StandardInventoryIterator sinkIterator() {
        return new StandardInventoryIterator(inv, side, getSlots(), allowedSlots) {

            @Override
            protected boolean canAccess(ItemStack stack, int slot) {
                return canInsert(stack, slot);
            }

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

    @Override
    public @Nullable InventoryIterator simulatedSinkIterator() {
        return new StandardInventoryIterator(inv, side, getSlots(), allowedSlots) {

            @Override
            protected boolean canAccess(ItemStack stack, int slot) {
                return canInsert(stack, slot);
            }

            @Override
            protected int getSlotStackLimit(int slot, ItemStack stack) {
                return InventoryItemSink.this.getSlotStackLimit(slot, stack);
            }

            @Override
            protected void setInventorySlotContents(int slot, ItemStack stack) {}

            @Override
            protected void markDirty() {}

            @Override
            protected boolean canExtract(ItemStack stack, int slot) {
                return false;
            }

            @Override
            public ItemStack extract(int amount, boolean forced) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ImmutableItemStack previous() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean rewind() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
