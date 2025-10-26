package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;

/**
 * A InventorySourceIterator implementation for something IInventory-like. This does not make any assumptions about the
 * backing inventory, it only implements the iterator portion of the logic. Implementations are free to do anything with
 * regard to {@link #getStackInSlot(int)} and {@link #setInventorySlotContents(int, ItemStack)}.
 */
public abstract class AbstractInventoryIterator implements InventoryIterator {

    private final int[] slots;

    private int i = 0, last = 0;

    private final FastImmutableItemStack pooled = new FastImmutableItemStack(null);

    protected AbstractInventoryIterator(int[] slots) {
        this.slots = slots;
    }

    private static final int[] NO_SLOTS = new int[0];

    protected AbstractInventoryIterator(int[] a, int[] b) {
        if (a == null && b == null) {
            this.slots = NO_SLOTS;
        } else if (a == null || b == null) {
            this.slots = a == null ? b : a;
        } else {
            this.slots = intersect(a, b);
        }
    }

    public static int[] intersect(int[] a, int[] b) {
        IntLinkedOpenHashSet a2 = new IntLinkedOpenHashSet(a);
        IntLinkedOpenHashSet b2 = new IntLinkedOpenHashSet(b);

        IntArrayList out = new IntArrayList();

        a2.forEach(i -> {
            if (b2.contains(i)) {
                out.add(i);
            }
        });

        return out.toIntArray();
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
     * Marks the backing inventory dirty.
     *
     * @see IInventory#markDirty()
     */
    protected void markDirty() {

    }

    protected boolean canExtract(ItemStack stack, int slot) {
        return true;
    }

    protected boolean canInsert(ItemStack stack, int slot) {
        return true;
    }

    @Override
    public boolean hasNext() {
        return i < slots.length;
    }

    @Override
    public ImmutableItemStack next() {
        last = i++;

        pooled.stack = getStackInSlot(slots[last]);
        return pooled.stack == null || !canExtract(pooled.stack, slots[last]) ? null : pooled;
    }

    @Override
    public boolean hasPrevious() {
        return i > 0;
    }

    @Override
    public ImmutableItemStack previous() {
        last = --i;

        pooled.stack = getStackInSlot(slots[last]);
        return pooled.stack == null || !canExtract(pooled.stack, slots[last]) ? null : pooled;
    }

    @Override
    public int nextIndex() {
        return slots[last + 1];
    }

    @Override
    public int previousIndex() {
        return slots[last - 1];
    }

    @Override
    public ItemStack extract(int amount) {
        ItemStack inSlot = getStackInSlot(slots[last]);

        if (ItemUtil.isStackEmpty(inSlot)) return null;
        if (!canExtract(inSlot, slots[last])) return null;

        int toExtract = Math.min(inSlot.stackSize, amount);

        ItemStack extracted = inSlot.splitStack(toExtract);

        setInventorySlotContents(slots[last], inSlot.stackSize == 0 ? null : inSlot);

        markDirty();

        return extracted;
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        if (ItemUtil.isStackEmpty(stack)) return null;

        stack = stack.copy();

        int slotIndex = getCurrentSlot();

        ItemStack inSlot = getStackInSlot(slotIndex);

        if (!ItemUtil.isStackEmpty(inSlot) && !ItemUtil.areStacksEqual(inSlot, stack)) {
            return stack;
        }

        if (!canInsert(stack, slotIndex)) {
            return stack;
        }

        if (!ItemUtil.isStackEmpty(inSlot)) {
            int maxStack = getSlotStackLimit(slotIndex, stack);
            int toInsert = Math.min(maxStack - inSlot.stackSize, stack.stackSize);

            inSlot.stackSize += toInsert;
            stack.stackSize -= toInsert;

            setInventorySlotContents(slotIndex, inSlot);

            markDirty();

            return stack.stackSize <= 0 ? null : stack;
        } else {
            setInventorySlotContents(slotIndex, stack);

            markDirty();

            return null;
        }
    }

    @Override
    public boolean rewind() {
        i = 0;

        return true;
    }

    protected abstract int getSlotStackLimit(int slot, ItemStack stack);

    protected final int getCurrentSlot() {
        return slots[last];
    }
}
