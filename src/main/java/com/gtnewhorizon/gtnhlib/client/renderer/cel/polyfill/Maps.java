package com.gtnewhorizon.gtnhlib.client.renderer.cel.polyfill;

import static it.unimi.dsi.fastutil.objects.Object2ObjectMaps.unmodifiable;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;

public class Maps {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Object2ObjectMap<K, V> copyOf(Map<K, V> input) {
        if (input instanceof Object2ObjectMaps.UnmodifiableMap u) return u;
        return unmodifiable(new Object2ObjectOpenHashMap<>(input));
    }
}
