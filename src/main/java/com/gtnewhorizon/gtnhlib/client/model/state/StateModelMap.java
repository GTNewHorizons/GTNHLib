package com.gtnewhorizon.gtnhlib.client.model.state;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

public interface StateModelMap {

    /// @param state The blockstate properties as strings
    /// @return The appropriate model, or [MissingModel#MISSING_MODEL] if none match
    @NotNull
    UnbakedModel selectModel(BlockState state);
}
