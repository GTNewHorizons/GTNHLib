package com.gtnewhorizon.gtnhlib.capability.item;

import java.util.OptionalInt;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrappedItemIO implements IItemIO {

    public final IItemSource source;
    public final IItemSink sink;

    public WrappedItemIO(IItemSource source, IItemSink sink) {
        this.source = source;
        this.sink = sink;
    }

    @Override
    public ItemStack store(ItemStack stack) {
        return sink == null ? stack : sink.store(stack);
    }

    @Override
    public void setAllowedSinkSlots(int @Nullable [] slots) {
        if (sink != null) sink.setAllowedSinkSlots(slots);
    }

    @Override
    public OptionalInt getStoredAmount(@Nullable ItemStack stack) {
        return sink == null ? OptionalInt.empty() : sink.getStoredAmount(stack);
    }

    @Override
    public WrappedItemIO then(IItemSink next) {
        return new WrappedItemIO(source, IItemSink.chain(sink, next));
    }

    @Override
    public @NotNull InventorySourceIterator iterator() {
        return source == null ? InventorySourceIterator.EMPTY : source.iterator();
    }

    @Override
    public void setAllowedSourceSlots(int @Nullable [] slots) {
        if (source != null) source.setAllowedSourceSlots(slots);
    }
}
