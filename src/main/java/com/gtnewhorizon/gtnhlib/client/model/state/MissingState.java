package com.gtnewhorizon.gtnhlib.client.model.state;

import com.gtnewhorizon.gtnhlib.client.model.UnbakedModel;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class MissingState implements StateModelMap {
    public static final MissingState MISSING_STATE_MAP = new MissingState();

    @Override
    public @Nullable UnbakedModel selectModel(Map<String, String> properties) {
        return null;
    }
}
