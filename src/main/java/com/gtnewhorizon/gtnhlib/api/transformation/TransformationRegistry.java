package com.gtnewhorizon.gtnhlib.api.transformation;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

final public class TransformationRegistry {

    private static final Map<String, ITransformation> TRANSFORMATION_MAP = new Object2ObjectOpenHashMap<>();

    public static ITransformation getTransformation(String id) {
        return TRANSFORMATION_MAP.get(id);
    }

    @SuppressWarnings("unused")
    public static void registerTransformation(@NotNull ITransformation transformation) {
        final String id = transformation.getId();
        if (TRANSFORMATION_MAP.containsKey(id)) {
            GTNHLib.LOG.error(
                    "Transformation ID {} is already registered. It is recommended to prefix transformation IDs with your mod ID.",
                    id);
        } else {
            TRANSFORMATION_MAP.put(id, transformation);
        }
    }
}
