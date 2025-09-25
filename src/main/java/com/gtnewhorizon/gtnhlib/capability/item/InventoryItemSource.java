package com.gtnewhorizon.gtnhlib.capability.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import gregtech.api.util.GTDataUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class InventoryItemSource implements IItemSource {

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
    public void setAllowedSourceSlots(int[] slots) {
        this.allowedSlots = slots;
    }

    @Override
    public @NotNull InventorySourceIterator iterator() {
        int[] effectiveSlots = allowedSlots != null ? GTDataUtils.intersect(slots, allowedSlots) : slots;

        return new AbstractInventorySourceIterator(effectiveSlots) {

            @Override
            protected ItemStack getStackInSlot(int slot) {
                return inv.getStackInSlot(slot);
            }

            @Override
            protected void setInventorySlotContents(int slot, ItemStack stack) {
                inv.setInventorySlotContents(slot, stack);
            }

            @Override
            protected void markDirty() {
                inv.markDirty();
            }

            @Override
            protected boolean canExtract(ItemStack stack, int slot) {
                if (inv instanceof ISidedInventory sided) {
                    return sided.canExtractItem(slot, stack, side.ordinal());
                } else {
                    return true;
                }
            }

            @Override
            protected boolean canInsert(ItemStack stack, int slot) {
                if (inv instanceof ISidedInventory sided) {
                    return sided.canInsertItem(slot, stack, side.ordinal());
                } else {
                    return true;
                }
            }
        };
    }
}
