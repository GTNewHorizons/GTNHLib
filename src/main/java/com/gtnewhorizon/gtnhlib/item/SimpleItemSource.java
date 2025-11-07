package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.item.ItemSource;

public abstract class SimpleItemSource implements ItemSource {

    protected int[] allowedSourceSlots;

    @Override
    public void resetSource() {
        allowedSourceSlots = null;
    }

    @Override
    public void setAllowedSourceSlots(int @Nullable [] slots) {
        allowedSourceSlots = slots;
    }

    @Override
    public @Nullable ItemStack pull(@Nullable ItemStackPredicate filter, @Nullable ItemStack2IntFunction amount) {
        InventoryIterator iter = sourceIterator();

        while (iter.hasNext()) {
            ImmutableItemStack stack = iter.next();

            if (stack == null || stack.isEmpty()) continue;

            if (filter == null || filter.test(stack)) {
                int toExtract = amount == null ? stack.getStackSize() : amount.apply(stack);

                return iter.extract(toExtract, false);
            }
        }

        return null;
    }

    @Override
    public @NotNull InventoryIterator sourceIterator() {
        return iterator(allowedSourceSlots);
    }

    @NotNull
    protected abstract InventoryIterator iterator(int[] allowedSlots);
}
