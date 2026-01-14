package com.gtnewhorizon.gtnhlib.item.impl.mfr;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.item.InventoryIterator;
import com.gtnewhorizon.gtnhlib.item.SimpleItemSource;

import powercrystals.minefactoryreloaded.api.IDeepStorageUnit;

public class DSUItemSource extends SimpleItemSource {

    public final IDeepStorageUnit dsu;

    public DSUItemSource(IDeepStorageUnit dsu) {
        this.dsu = dsu;
    }

    @Override
    protected @NotNull InventoryIterator iterator(int[] allowedSlots) {
        return new DSUInventoryIterator(dsu, allowedSlots, false);
    }
}
