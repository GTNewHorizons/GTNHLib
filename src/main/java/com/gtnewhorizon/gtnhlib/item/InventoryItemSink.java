package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.primitives.Ints;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;

public class InventoryItemSink implements ItemSink {

    private static final ClassValue<Boolean> STANDARD_STORAGE = new ClassValue<>() {

        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("store", ImmutableItemStack.class).getDeclaringClass() == InventoryItemSink.class
                        && type.getMethod("sinkIterator").getDeclaringClass() == InventoryItemSink.class;
            } catch (NoSuchMethodException | SecurityException | LinkageError ignored) {
                return false;
            }
        }
    };

    public final IInventory inv;
    private final ForgeDirection side;
    private final boolean standardStorage;

    private int[] allowedSlots;
    private int slotStackLimit = Integer.MAX_VALUE;

    public InventoryItemSink(IInventory inv, ForgeDirection side) {
        this.inv = inv;
        this.side = side;
        standardStorage = STANDARD_STORAGE.get(getClass());
    }

    protected int[] getSlots() {
        return StandardInventoryIterator.getInventorySlotIndices(inv, side);
    }

    @Override
    public void resetSink() {
        ItemSink.super.resetSink();
        allowedSlots = null;
        slotStackLimit = Integer.MAX_VALUE;
    }

    @Override
    public void setAllowedSinkSlots(int @Nullable [] slots) {
        this.allowedSlots = slots;
    }

    @Override
    public void setSlotStackLimit(int limit) {
        this.slotStackLimit = limit;
    }

    @Override
    public boolean mayAcceptItems() {
        // Existing subclasses can accept items independently of the visible inventory's capacity
        if (!standardStorage) return true;
        if (slotStackLimit <= 0) return false;
        // An inaccessible slot can provide a positive hint. The insertion iterator still checks slot permissions.
        if (inv.getSizeInventory() > 0 && hasCapacity(0)) return true;
        for (int slot : getSlots()) {
            if (hasCapacity(slot)) return true;
        }
        return false;
    }

    private boolean hasCapacity(int slot) {
        ItemStack stored = inv.getStackInSlot(slot);
        if (stored == null || stored.stackSize <= 0) return true;
        // Avoid querying slot capacity for a single unstackable item
        if (stored.stackSize == 1) {
            return stored.isStackable() && stored.stackSize < Math.min(getSlotStackLimit(slot, stored), slotStackLimit);
        }
        return stored.stackSize < Math.min(getSlotStackLimit(slot, stored), slotStackLimit) && stored.isStackable();
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
                return Math.min(InventoryItemSink.this.getSlotStackLimit(slot, stack), slotStackLimit);
            }
        };
    }

    protected int getSlotStackLimit(int slot, ItemStack stack) {
        int invStackLimit = inv.getInventoryStackLimit();

        int existingMaxStack = stack == null ? 64 : stack.getMaxStackSize();

        int baseLimit;
        if (invStackLimit > 64) {
            baseLimit = Math.min(invStackLimit, Ints.saturatedCast((long) (invStackLimit / 64) * existingMaxStack));
        } else {
            baseLimit = Math.min(invStackLimit, existingMaxStack);
        }

        return Math.min(baseLimit, slotStackLimit);
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
                return Math.min(InventoryItemSink.this.getSlotStackLimit(slot, stack), slotStackLimit);
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
                GTNHLib.LOG.warn("This simulated sink iterator doesn't support backward traversal");
                return null;
            }

            @Override
            public boolean rewind() {
                return false;
            }
        };
    }
}
