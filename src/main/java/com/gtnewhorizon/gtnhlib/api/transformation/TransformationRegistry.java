package com.gtnewhorizon.gtnhlib.api.transformation;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

final public class TransformationRegistry {

    private static final Map<String, ITransformation> TRANSFORMATION_MAP = new Object2ObjectOpenHashMap<>();

    /**
     * Get a registered transformation
     * @param id The ID of the transformation to get
     * @return the transformation, or null if it cannot be found
     */
    @Nullable
    public static ITransformation getTransformation(String id) {
        return TRANSFORMATION_MAP.get(id);
    }

    /**
     * Registers the transformation with the game. {@link ITransformation#getId() The transformation's ID} must be
     * unique. It is recommended to prefix the ID with your mod's name to avoid collisions.
     * @param transformation The new transformation to add
     */
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
