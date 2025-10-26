package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.item.ItemSource;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class InventoryItemSource implements ItemSource {

    public final IInventory inv;
    public final ForgeDirection side;

    private final int[] slots;
    private int[] allowedSlots;

    public InventoryItemSource(IInventory inv, ForgeDirection side) {
        this.inv = inv;
        this.side = side;

        int invLength = inv.getSizeInventory();

        IntArrayList slots = new IntArrayList();

        if (inv instanceof ISidedInventory sided) {
            for (int slot : sided.getAccessibleSlotsFromSide(side.ordinal())) {
                slots.add(slot);
            }
        } else {
            for (int slot = 0; slot < invLength; slot++) {
                slots.add(slot);
            }
        }

        this.slots = slots.toIntArray();
    }

    @Override
    public void resetSource() {
        ItemSource.super.resetSource();
        allowedSlots = null;
    }

    @Override
    public void setAllowedSourceSlots(int[] slots) {
        this.allowedSlots = slots;
    }

    @Override
    public @Nullable ItemStack pull(@Nullable ItemStackPredicate filter, @Nullable ItemStack2IntFunction amount) {
        StandardInventoryIterator iter = sourceIterator();

        while (iter.hasNext()) {
            ImmutableItemStack slot = iter.next();

            if (slot == null || slot.isEmpty()) continue;

            if (filter == null || filter.test(slot)) {
                int toExtract = amount == null ? slot.getStackSize() : amount.apply(slot);

                if (toExtract > 0) {
                    return iter.extract(toExtract);
                }
            }
        }

        return null;
    }

    @Override
    public @NotNull StandardInventoryIterator sourceIterator() {
        return new StandardInventoryIterator(inv, side, slots, allowedSlots) {

            @Override
            protected int getSlotStackLimit(int slot, ItemStack stack) {
                return InventoryItemSource.this.getSlotStackLimit(slot, stack);
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
