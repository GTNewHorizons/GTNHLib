package com.gtnewhorizon.gtnhlib.client.model.state;

import com.gtnewhorizon.gtnhlib.client.model.UnbakedModel;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class Missing implements StateModelMap {
    public static final Missing MISSING_MAP = new Missing();

    @Override
    public @Nullable UnbakedModel selectModel(Map<String, String> properties) {
        return null;
    }
}
