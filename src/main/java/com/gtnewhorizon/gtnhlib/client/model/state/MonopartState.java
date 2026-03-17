package com.gtnewhorizon.gtnhlib.client.model.state;

import java.util.Map;
import java.util.Objects;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.MonopartDough;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;

public class MonopartState implements StateModelMap {

    private final Object2ObjectMap<StateMatch, ObjectList<Weighted<JSONVariant>>> variants;

    MonopartState(Object2ObjectMap<StateMatch, ObjectList<Weighted<JSONVariant>>> variants) {
        this.variants = Object2ObjectMaps.unmodifiable(variants);
    }

    @Override
    public UnbakedModel selectModel(BlockState state) {
        Map<String, String> properties = state.toMap();

        final var iter = Object2ObjectMaps.fastIterator(variants);
        while (iter.hasNext()) {
            final var e = iter.next();
            final var match = e.getKey();
            if (match.matches(properties)) return new MonopartDough(e.getValue());
        }

        return null;
    }

    static class StateMatch {

        @Getter
        private final String variantName;
        private final boolean matchAll;
        private final Object2ObjectMap<String, String> states;

        public StateMatch(String s) {
            variantName = s;

            if (s.isEmpty()) {
                matchAll = true;
                states = null;
                return;
            }

            matchAll = false;
            states = new Object2ObjectOpenHashMap<>();

            for (String c : s.split(",")) {
                final int eqIndex = c.indexOf("=");

                if (eqIndex == -1) throw new RuntimeException(
                        "Model variant predicate is missing an equals; expected it to be of the format 'property name=property value': '"
                                + c
                                + "'!");

                states.put(c.substring(0, eqIndex), c.substring(eqIndex + 1));
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
