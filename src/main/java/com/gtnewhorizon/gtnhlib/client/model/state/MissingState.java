package com.gtnewhorizon.gtnhlib.client.model.state;

import static com.gtnewhorizon.gtnhlib.client.model.unbaked.MissingModel.MISSING_MODEL;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

public class MissingState implements StateModelMap {

    public static final MissingState MISSING_STATE_MAP = new MissingState();

    @Override
    public @NotNull UnbakedModel selectModel(BlockState state) {
        return MISSING_MODEL;
    }
}
