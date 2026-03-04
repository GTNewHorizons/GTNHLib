package com.gtnewhorizon.gtnhlib.client.model.state;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

public interface StateModelMap {

    /**
     * @param state The blockstate properties as strings
     * @return The appropriate model, or null if none match
     */
    UnbakedModel getModel(BlockState state);
}
