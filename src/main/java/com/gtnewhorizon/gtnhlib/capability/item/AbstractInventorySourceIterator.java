package com.gtnewhorizon.gtnhlib.capability.item;

import java.util.Objects;

import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * A InventorySourceIterator implementation for something IInventory-like.
 */
public abstract class AbstractInventorySourceIterator implements InventorySourceIterator {

    private final int[] slots;

    private int i = 0, last = 0;
    private boolean dirty = false;

    private final FastImmutableItemStack pooled = new FastImmutableItemStack(null);

    protected AbstractInventorySourceIterator(int[] slots) {
        this.slots = slots;
    }

    /**
     * @see IInventory#getStackInSlot(int)
     */
    protected abstract ItemStack getStackInSlot(int slot);

    /**
     * @see IInventory#setInventorySlotContents(int, ItemStack)
     */
    protected abstract void setInventorySlotContents(int slot, ItemStack stack);

    /**
     * Marks the backing inventory dirty. Only called the first time a stack is set.
     * @see IInventory#markDirty()
     */
    protected void markDirty() {

    }

    @Override
    public boolean hasNext() {
        return i < slots.length;
    }

    @Override
    public ImmutableItemStack next() {
        last = i++;

        pooled.stack = getStackInSlot(slots[last]);
        return pooled.stack == null ? null : pooled;
    }

    @Override
    public boolean hasPrevious() {
        return i > 0;
    }

    @Override
    public ImmutableItemStack previous() {
        last = --i;

        pooled.stack = getStackInSlot(slots[last]);
        return pooled.stack == null ? null : pooled;
    }

    @Override
    public int nextIndex() {
        return i + 1;
    }

    @Override
    public int previousIndex() {
        return i - 1;
    }

    @Override
    public ItemStack extract(int amount) {
        ItemStack inSlot = getStackInSlot(slots[last]);

        if (!isStackValid(inSlot)) return null;

        int toExtract = Math.min(inSlot.stackSize, amount);

        ItemStack extracted = inSlot.splitStack(toExtract);

        setInventorySlotContents(slots[last], inSlot.stackSize == 0 ? null : inSlot);

        if (!dirty) {
            markDirty();
            dirty = true;
        }

        return extracted;
    }

    @Override
    public void insert(ItemStack stack) {
        if (!isStackValid(stack)) return;

        ItemStack inSlot = getStackInSlot(slots[last]);

        if (isStackValid(inSlot) && !areStacksEqual(inSlot, stack)) {
            throw new IllegalArgumentException("Cannot insert stack that does not match the existing stack. Attempted to inject: " + stack + ", already had: " + inSlot);
        }

        ItemStack out;

        if (isStackValid(inSlot)) {
            out = copyAmount(inSlot.stackSize + stack.stackSize, inSlot);
        } else {
            out = stack;
        }

        setInventorySlotContents(slots[last], out);
    }

    protected final int getCurrentSlot() {
        return slots[last];
    }

    private static boolean isStackValid(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.stackSize > 0;
    }

    private static boolean areStacksEqual(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;

        if (a.getItem() != b.getItem()) return false;
        if (Items.feather.getDamage(a) != Items.feather.getDamage(b)) return false;
        if (!Objects.equals(a.getTagCompound(), b.getTagCompound())) return false;

        return true;
    }

    private static ItemStack copyAmount(int amount, ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;

        stack = stack.copy();
        stack.stackSize = amount;

        return stack;
    }
}
