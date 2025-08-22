package com.gtnewhorizon.gtnhlib.json;

import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MonopartState implements StateDef {
    private final Object2ObjectMap<StateMatch, ObjectList<JSONVariant>> variants;

    MonopartState(Object2ObjectMap<StateMatch, ObjectList<JSONVariant>> variants) {
        this.variants = Object2ObjectMaps.unmodifiable(variants);
    }

    public JSONVariant getModelLocation(Map<String, String> properties, Random rand) {
        final var iter = Object2ObjectMaps.fastIterator(variants);
        while (iter.hasNext()) {
            final var e = iter.next();
            final var match = e.getKey();

            if (match.matches(properties)) return selectOne(e.getValue(), rand);
        }

        return null;
    }

    private JSONVariant selectOne(ObjectList<JSONVariant> jsonVariants, Random rand) {
        var weight = 0;
        for (var v : jsonVariants) {
            weight += v.weight;
        }

        final var selector = rand.nextInt(weight);
        weight = 0;
        for (var v : jsonVariants) {
            if (selector <= weight) return v;
            weight += v.weight;
        }

        throw new IllegalStateException("Randomly selected beyond the list!");
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

                states.put(c.substring(0, eidx), c.substring(eidx));
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
