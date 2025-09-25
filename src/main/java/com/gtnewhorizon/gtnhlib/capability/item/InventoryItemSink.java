package com.gtnewhorizon.gtnhlib.capability.item;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.ArrayListMultimap;
import com.gtnewhorizon.gtnhlib.datastructs.extensions.IterableBitSet;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;

public class InventoryItemSink implements IItemSink {

    public final IInventory inv;
    private final ForgeDirection side;

    private final ArrayListMultimap<ItemId, SlotInfo> partialItemStacks = ArrayListMultimap.create();

    private final IterableBitSet emptySlots = new IterableBitSet();

    private boolean markedDirty = false;

    public InventoryItemSink(IInventory inv, ForgeDirection side) {
        this.inv = inv;
        this.side = side;

        IntStream slots;

        if (inv instanceof ISidedInventory sided) {
            slots = IntStream.of(sided.getAccessibleSlotsFromSide(side.ordinal()));
        } else {
            slots = IntStream.range(0, inv.getSizeInventory());
        }

        slots.forEach(slot -> {
            ItemStack existing = inv.getStackInSlot(slot);

            int maxStack = getSlotStackLimit(slot, existing);

            if (ItemUtil.isStackEmpty(existing)) {
                emptySlots.set(slot);
            } else if (existing.stackSize < maxStack) {
                SlotInfo slotInfo = new SlotInfo(
                        inv,
                        slot,
                        inv.getStackInSlot(slot),
                        getSlotStackLimit(slot, existing));
                partialItemStacks.put(ItemId.create(existing), slotInfo);
            }
        });
    }

    @Override
    public ItemStack store(ItemStack stack) {
        if (stack.stackSize <= 0) return null;

        ItemId id = ItemId.create(stack);

        List<SlotInfo> partialSlots = partialItemStacks.get(id);

        Iterator<SlotInfo> iterator = partialSlots.iterator();
        while (iterator.hasNext()) {
            if (stack.stackSize <= 0) return null;

            SlotInfo slot = iterator.next();

            if (!inv.isItemValidForSlot(slot.getSlot(), stack)) continue;

            if (side != ForgeDirection.UNKNOWN && inv instanceof ISidedInventory sided) {
                if (!sided.canInsertItem(slot.getSlot(), stack, side.ordinal())) continue;
            }

            // Safety in case the slot changed since we cached it
            slot.contents = inv.getStackInSlot(slot.getSlot());
            if (!ItemUtil.areStacksEqual(slot.contents, stack)) {
                iterator.remove();
                continue;
            }

            int remaining = slot.maxStackSize - slot.contents.stackSize;

            if (remaining <= 0) {
                iterator.remove();
                continue;
            }

            int toTransfer = Math.min(remaining, stack.stackSize);

            stack.stackSize -= toTransfer;
            slot.contents.stackSize += toTransfer;

            markDirty();

            if (slot.contents.stackSize >= slot.maxStackSize) {
                iterator.remove();
            }
        }

        if (stack.stackSize <= 0) return null;

        for (int slot : emptySlots) {
            if (stack.stackSize <= 0) return null;

            if (!inv.isItemValidForSlot(slot, stack)) continue;

            if (side != ForgeDirection.UNKNOWN && inv instanceof ISidedInventory sided) {
                if (!sided.canInsertItem(slot, stack, side.ordinal())) continue;
            }

            // Safety in case the slot changed since we cached it
            if (inv.getStackInSlot(slot) != null) {
                emptySlots.clear(slot);
                continue;
            }

            int maxStack = getSlotStackLimit(slot, stack);

            int toTransfer = Math.min(maxStack, stack.stackSize);

            ItemStack inserted = ItemUtil.copyAmount(toTransfer, stack);
            inv.setInventorySlotContents(slot, inserted);
            stack.stackSize -= toTransfer;

            if (toTransfer < maxStack) {
                SlotInfo slotInfo = new SlotInfo(inv, slot, inv.getStackInSlot(slot), getSlotStackLimit(slot, stack));
                partialItemStacks.put(id, slotInfo);
            }

            emptySlots.clear(slot);
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
