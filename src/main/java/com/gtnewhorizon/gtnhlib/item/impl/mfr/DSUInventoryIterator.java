package com.gtnewhorizon.gtnhlib.item.impl.mfr;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.item.AbstractInventoryIterator;
import com.gtnewhorizon.gtnhlib.item.ImmutableItemStack;
import com.gtnewhorizon.gtnhlib.util.ItemUtil;

import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

public class DSUInventoryIterator extends AbstractInventoryIterator {

    private static final int[] SLOTS = { 0 };

    public final IDeepStorageUnit dsu;

    public DSUInventoryIterator(IDeepStorageUnit dsu, int[] allowedSlots) {
        super(SLOTS, allowedSlots);
        this.dsu = dsu;
    }

    @Override
    protected ItemStack getStackInSlot(int slot) {
        if (slot != 0) return null;

        return ItemUtil.copy(dsu.getStoredItemType());
    }

    @Override
    public ItemStack extract(int amount, boolean forced) {
        ItemStack stored = dsu.getStoredItemType();

        if (stored == null) return null;

        int toExtract = Math.min(amount, stored.stackSize);

        dsu.setStoredItemCount(stored.stackSize - toExtract);

        return ItemUtil.copyAmount(toExtract, stored);
    }

    @Override
    public int insert(ImmutableItemStack stack, boolean forced) {
        ItemStack stored = dsu.getStoredItemType();

        if (stored != null && !stack.matches(stored)) return stack.getStackSize();

        int storedAmount = stored == null ? 0 : stored.stackSize;
        int toInsert = Math.min(stack.getStackSize(), dsu.getMaxStoredCount() - storedAmount);

        if (stored == null) {
            dsu.setStoredItemType(stack.toStack(1), 1);
        }

        dsu.setStoredItemCount(storedAmount + toInsert);

        return stack.getStackSize() - toInsert;
    }
}
