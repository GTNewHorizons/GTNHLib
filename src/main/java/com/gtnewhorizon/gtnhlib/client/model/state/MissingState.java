package com.gtnewhorizon.gtnhlib.client.model.state;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

public class MissingState implements StateModelMap {

    public static final MissingState MISSING_STATE_MAP = new MissingState();

    @Override
    public @Nullable String selectVariant(BlockState state) {
        return null;
    }

    @Override
    public UnbakedModel getModel(String variantName) {
        return null;
    }
}
