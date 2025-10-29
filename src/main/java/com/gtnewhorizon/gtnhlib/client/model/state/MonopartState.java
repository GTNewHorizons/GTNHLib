package com.gtnewhorizon.gtnhlib.client.model.state;

import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.MonopartDough;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class MonopartState implements StateModelMap {

    private final Object2ObjectMap<StateMatch, ObjectList<Weighted<JSONVariant>>> variants;

    MonopartState(Object2ObjectMap<StateMatch, ObjectList<Weighted<JSONVariant>>> variants) {
        this.variants = Object2ObjectMaps.unmodifiable(variants);
    }

    @Override
    public @Nullable UnbakedModel selectModel(Map<String, String> properties) {
        final var iter = Object2ObjectMaps.fastIterator(variants);
        while (iter.hasNext()) {
            final var e = iter.next();
            final var match = e.getKey();

            if (match.matches(properties)) return new MonopartDough(e.getValue());
        }

        return null;
    }

    static class StateMatch {

        private final boolean matchAll;
        private final Object2ObjectMap<String, String> states;

        StateMatch(String s) {
            if (s.isEmpty()) {
                matchAll = true;
                states = null;
                return;
            }
            matchAll = false;
            states = new Object2ObjectOpenHashMap<>();

            final var cases = s.split(",");
            for (var c : cases) {
                final int eidx = c.indexOf("=");
                if (eidx == -1) throw new RuntimeException("Unexpected blockstate case '" + c + "'!");

                states.put(c.substring(0, eidx), c.substring(eidx + 1));
            }
        }

        boolean matches(Map<String, String> properties) {
            if (matchAll) return true;

            final var iter = Object2ObjectMaps.fastIterator(states);
            while (iter.hasNext()) {
                final var e = iter.next();
                if (!Objects.equals(properties.get(e.getKey()), e.getValue())) return false;
            }

            return true;
        }
    }
}
