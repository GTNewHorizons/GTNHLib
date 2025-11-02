package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;

/// An [InventoryIterator] implementation for something [IInventory]-like. This does not make any assumptions about the
/// backing inventory, it only implements the iterator portion of the logic. When an item was extracted from a slot, it
/// must be force-insertable back into that same slot, otherwise [ItemTransfer#transfer()] will drop or void the stack.
public abstract class AbstractInventoryIterator implements InventoryIterator {

    public static final int[] NO_SLOTS = new int[0];

    private final int[] slots;

    private int i = 0, last = 0;

    private final FastImmutableItemStack pooled = new FastImmutableItemStack(null);

    protected AbstractInventoryIterator(int[] slots) {
        this.slots = slots;
    }

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

    protected boolean canExtract(ItemStack stack, int slot) {
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
    public boolean rewind() {
        i = 0;

        return true;
    }

    protected final int getCurrentSlot() {
        return slots[last];
    }
}
