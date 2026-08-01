package com.gtnewhorizon.gtnhlib.functional;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Compute3DWithValue<V> {

    /// @param currentValue the value currently mapped at (posX, posY, posZ), or null if absent
    /// @return the new value to map, or null to remove the mapping
    @Nullable
    V apply(int posX, int posY, int posZ, @Nullable V currentValue);
}
