package com.gtnewhorizon.gtnhlib.item.impl.mfr;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.item.InventoryIterator;
import com.gtnewhorizon.gtnhlib.item.SimpleItemIO;

import org.jetbrains.annotations.Nullable;
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
}
