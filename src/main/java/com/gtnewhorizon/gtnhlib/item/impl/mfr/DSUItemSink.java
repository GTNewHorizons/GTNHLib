package com.gtnewhorizon.gtnhlib.item.impl.mfr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.item.InventoryIterator;
import com.gtnewhorizon.gtnhlib.item.SimpleItemSink;

import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

public class DSUItemSink extends SimpleItemSink {

    public final IDeepStorageUnit dsu;

    public DSUItemSink(IDeepStorageUnit dsu) {
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
}
