package com.gtnewhorizon.gtnhlib.client.model.state;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

public class MissingState implements StateModelMap {

    public static final MissingState MISSING_STATE_MAP = new MissingState();

    @Override
    public UnbakedModel selectModel(BlockState state) {
        return null;
    }
}
