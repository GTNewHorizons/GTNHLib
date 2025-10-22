package com.gtnewhorizon.gtnhlib.client.model.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

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

    private final List<StateMatch> variants = new ArrayList<>();
    private final Object2ObjectMap<String, ObjectList<Weighted<JSONVariant>>> models = new Object2ObjectOpenHashMap<>();

    MonopartState(Object2ObjectMap<StateMatch, ObjectList<Weighted<JSONVariant>>> variants) {
        variants.forEach((match, model) -> {
            this.variants.add(match);
            this.models.put(match.variantName, model);
        });
    }

    @Override
    public @Nullable String selectVariant(BlockState state) {
        Map<String, String> props = state.toMap();

        for (StateMatch match : variants) {
            if (match.matches(props)) return match.variantName;
        }

        return null;
    }

    @Override
    public UnbakedModel getModel(String variantName) {
        return new MonopartDough(models.get(variantName));
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

                if (eqIndex == -1) throw new RuntimeException("Model variant predicate is missing an equals; expected it to be of the format 'property name=property value': '" + c + "'!");

                states.put(c.substring(0, eqIndex), c.substring(eqIndex+1));
            }
        }

        public boolean matches(Map<String, String> properties) {
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
