package com.gtnewhorizon.gtnhlib.item.impl.mfr;

import java.util.OptionalInt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.item.ImmutableItemStack;
import com.gtnewhorizon.gtnhlib.item.InventoryIterator;
import com.gtnewhorizon.gtnhlib.item.SimpleItemIO;

import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

public class DSUItemIO extends SimpleItemIO {

    public final IDeepStorageUnit dsu;

    public DSUItemIO(IDeepStorageUnit dsu) {
        this.dsu = dsu;
    }

    @Override
    protected @NotNull InventoryIterator iterator(int[] allowedSlots) {
        return new DSUInventoryIterator(dsu, allowedSlots, false);
    }

    @Override
    public @Nullable InventoryIterator simulatedSinkIterator() {
        return new DSUInventoryIterator(dsu, allowedSinkSlots, true);
    }

    @Override
    public OptionalInt simulateStore(ImmutableItemStack stack) {
        return OptionalInt.of(DSUInventoryIterator.simulateStore(dsu, allowedSinkSlots, stack));
    }
}
