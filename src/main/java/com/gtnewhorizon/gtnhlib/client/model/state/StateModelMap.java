package com.gtnewhorizon.gtnhlib.client.model.state;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

public interface StateModelMap {

    /**
     * @param state The blockstate properties as strings
     * @return The appropriate model, or null if none match
     */
    @Nullable
    String selectVariant(BlockState state);

    UnbakedModel getModel(String variantName);
}
