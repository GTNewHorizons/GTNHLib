package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * An inventory iterator for a standard inventory. Performs all item slots validation. When the side is
 * {@link ForgeDirection#UNKNOWN}, canExtractItem and canInsertItem are skipped - only
 * {@link IInventory#isItemValidForSlot(int, ItemStack)} is checked.
 */
public class StandardInventoryIterator extends AbstractInventoryIterator {

    private final IInventory inv;
    private final ForgeDirection side;

    private boolean markedDirty = false;

    public StandardInventoryIterator(IInventory inv, ForgeDirection side, int[] allowedSlots) {
        super(getInventorySlotIndices(inv, side), allowedSlots);

        this.inv = inv;
        this.side = side;
    }

    public StandardInventoryIterator(IInventory inv, ForgeDirection side, int[] inventorySlots, int[] allowedSlots) {
        super(inventorySlots, allowedSlots);

        this.inv = inv;
        this.side = side;
    }

    public static int[] getInventorySlotIndices(IInventory inv, ForgeDirection side) {
        if (inv instanceof ISidedInventory sided) {
            return sided.getAccessibleSlotsFromSide(side.ordinal());
        } else {
            int[] slots = new int[inv.getSizeInventory()];

            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }

            return slots;
        }
    }

    @Override
    protected ItemStack getStackInSlot(int slot) {
        return inv.getStackInSlot(slot);
    }

    @Override
    protected void setInventorySlotContents(int slot, ItemStack stack) {
        inv.setInventorySlotContents(slot, stack);
    }

    @Override
    protected boolean canExtract(ItemStack stack, int slot) {
        if (!inv.isItemValidForSlot(slot, stack)) return false;

        if (inv instanceof ISidedInventory sided) {
            if (side == ForgeDirection.UNKNOWN) return true;
            return sided.canExtractItem(slot, stack, side.ordinal());
        } else {
            return true;
        }
    }

    @Override
    protected boolean canInsert(ItemStack stack, int slot) {
        if (!inv.isItemValidForSlot(slot, stack)) return false;

        if (inv instanceof ISidedInventory sided) {
            if (side == ForgeDirection.UNKNOWN) return true;
            return sided.canInsertItem(slot, stack, side.ordinal());
        } else {
            return true;
        }
    }

    @Override
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
    protected void markDirty() {
        if (markedDirty) return;

        // Only mark the inv dirty once, iterators should never last longer than one tick.
        inv.markDirty();
        markedDirty = true;
    }
}
