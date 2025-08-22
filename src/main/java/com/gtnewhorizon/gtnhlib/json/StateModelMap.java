package com.gtnewhorizon.gtnhlib.json;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.client.model.UnbakedModel;

public interface StateModelMap {

    /**
     * @param properties The blockstate properties as strings
     * @return The appropriate model, or null if none match
     */
    @Nullable
    UnbakedModel selectModel(Map<String, String> properties);
}
