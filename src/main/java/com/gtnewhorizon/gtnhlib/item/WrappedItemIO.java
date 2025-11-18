package com.gtnewhorizon.gtnhlib.item;

import java.util.OptionalInt;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.item.ItemIO;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSource;

public class WrappedItemIO implements ItemIO {

    @Nullable
    public final ItemSource source;
    @Nullable
    public final ItemSink sink;

    public WrappedItemIO(@Nullable ItemSource source, @Nullable ItemSink sink) {
        this.source = source;
        this.sink = sink;
    }

    @Override
    public void resetSink() {
        if (sink != null) sink.resetSink();
    }

    @Override
    public int store(ImmutableItemStack stack) {
        return sink == null ? stack.getStackSize() : sink.store(stack);
    }

    @Override
    public void setAllowedSinkSlots(int @Nullable [] slots) {
        if (sink != null) sink.setAllowedSinkSlots(slots);
    }

    @Override
    public OptionalInt getStoredItemsInSink(@Nullable ItemStackPredicate filter) {
        return sink == null ? OptionalInt.empty() : sink.getStoredItemsInSink(filter);
    }

    @Override
    public @Nullable InventoryIterator sinkIterator() {
        return sink == null ? InventoryIterator.EMPTY : sink.sinkIterator();
    }

    @Override
    public WrappedItemIO then(ItemSink next) {
        return new WrappedItemIO(source, ItemSink.chain(sink, next));
    }

    @Override
    public void resetSource() {
        if (source != null) source.resetSource();
    }

    @Override
    public @Nullable ItemStack pull(@Nullable ItemStackPredicate filter, @Nullable ItemStack2IntFunction amount) {
        return source == null ? null : source.pull(filter, amount);
    }

    @Override
    public @Nullable InventoryIterator sourceIterator() {
        return source == null ? InventoryIterator.EMPTY : source.sourceIterator();
    }

    @Override
    public void setAllowedSourceSlots(int @Nullable [] slots) {
        if (source != null) source.setAllowedSourceSlots(slots);
    }
}
