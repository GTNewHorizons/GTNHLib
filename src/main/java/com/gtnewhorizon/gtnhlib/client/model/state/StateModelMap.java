package com.gtnewhorizon.gtnhlib.client.model.state;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

public interface StateModelMap {

    /**
     * @param state The block state
     * @return The appropriate model's variant name, or null if no matches exist.
     */
    @Nullable
    String selectVariant(BlockState state);

    UnbakedModel getModel(String variantName);
}
