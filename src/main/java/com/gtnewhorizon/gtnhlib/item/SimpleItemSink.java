package com.gtnewhorizon.gtnhlib.item;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;

public abstract class SimpleItemSink implements ItemSink {

    protected int[] allowedSinkSlots;

    @Override
    public void resetSink() {
        allowedSinkSlots = null;
    }

    @Override
    public void setAllowedSinkSlots(int @Nullable [] slots) {
        allowedSinkSlots = slots;
    }

    @Override
    public int store(ImmutableItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        InventoryIterator iter = sinkIterator();

        InsertionItemStack insertion = new InsertionItemStack(stack);

        while (iter.hasNext()) {
            iter.next();

            insertion.set(iter.insert(insertion, false));

            if (insertion.isEmpty()) return 0;
        }

        return insertion.getStackSize();
    }

    @Override
    public @NotNull InventoryIterator sinkIterator() {
        return iterator(allowedSinkSlots);
    }

    @NotNull
    protected abstract InventoryIterator iterator(int[] allowedSlots);

    @Override
    public abstract @Nullable InventoryIterator simulatedSinkIterator();
}
