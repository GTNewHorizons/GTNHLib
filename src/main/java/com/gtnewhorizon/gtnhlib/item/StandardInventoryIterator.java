package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.util.ItemUtil;

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
    public ItemStack extract(int amount, boolean forced) {
        int slotIndex = getCurrentSlot();

        ItemStack inSlot = getStackInSlot(slotIndex);

        if (ItemUtil.isStackEmpty(inSlot)) return null;
        if (!forced && !canExtract(inSlot, slotIndex)) return null;

        int toExtract = Math.min(inSlot.stackSize, amount);

        ItemStack extracted = inSlot.splitStack(toExtract);

        setInventorySlotContents(slotIndex, inSlot.stackSize == 0 ? null : inSlot);

        markDirty();

        return extracted;
    }

    @Override
    public int insert(ImmutableItemStack stack, boolean forced) {
        if (stack.isEmpty()) return 0;

        int slotIndex = getCurrentSlot();

        ItemStack inSlot = getStackInSlot(slotIndex);

        if (!ItemUtil.isStackEmpty(inSlot) && !stack.matches(inSlot)) {
            return stack.getStackSize();
        }

        ItemStack partialCopy = stack.toStackFast();

        if (!forced && !canInsert(partialCopy, slotIndex)) {
            return stack.getStackSize();
        }

        if (!ItemUtil.isStackEmpty(inSlot)) {
            int maxStack = getSlotStackLimit(slotIndex, partialCopy);
            int toInsert = forced ? stack.getStackSize() : Math.min(maxStack - inSlot.stackSize, stack.getStackSize());

            inSlot.stackSize += toInsert;

            setInventorySlotContents(slotIndex, inSlot);

            markDirty();

            return stack.getStackSize() - toInsert;
        } else {
            setInventorySlotContents(slotIndex, stack.toStack());

            markDirty();

            return 0;
        }
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

    protected void markDirty() {
        if (markedDirty) return;

        // Only mark the inv dirty once, iterators should never last longer than one tick.
        inv.markDirty();
        markedDirty = true;
    }
}
