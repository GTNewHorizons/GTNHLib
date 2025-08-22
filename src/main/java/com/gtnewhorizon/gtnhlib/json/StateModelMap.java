package com.gtnewhorizon.gtnhlib.json;

import com.gtnewhorizon.gtnhlib.client.model.UnbakedModel;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface StateModelMap {
    /**
     * @param properties The blockstate properties as strings
     * @return The appropriate model, or null if none match
     */
    @Nullable
    UnbakedModel selectModel(Map<String, String> properties);
}
