package com.gtnewhorizon.gtnhlib.functional;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Compute2DWithValue<V> {

    /// @param currentValue the value currently mapped at (posX, posZ), or null if absent
    /// @return the new value to map, or null to remove the mapping
    @Nullable
    V apply(int posX, int posZ, @Nullable V currentValue);
}
