package com.gtnewhorizon.gtnhlib.json;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.block.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.Variant;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class BlockStateDef {
    private final Object2ObjectMap<StateMatch, ObjectList<Variant>> variants;
    private final ObjectList<Case> multipart;

    BlockStateDef(Object2ObjectMap<StateMatch, ObjectList<Variant>> variants) {
        this.variants = Object2ObjectMaps.unmodifiable(variants);
        multipart = null;
    }

    BlockStateDef(ObjectList<Case> multipart) {
        variants = null;
        this.multipart = ObjectLists.unmodifiable(multipart);
    }

    public Variant getModelLocation(Map<String, String> properties, Random rand) {
        if (variants == null) return getModelLocationMultipart(properties);
        return getModelLocationVariants(properties, rand);
    }

    private Variant getModelLocationVariants(Map<String, String> properties, Random rand) {
        final var iter = Object2ObjectMaps.fastIterator(variants);
        while (iter.hasNext()) {
            final var e = iter.next();
            final var match = e.getKey();

            if (match.matches(properties)) return selectOne(e.getValue(), rand);
        }

        return null;
    }

    private Variant getModelLocationMultipart(Map<String, String> properties) {
    }

    private Variant selectOne(ObjectList<Variant> variants, Random rand) {
        var weight = 0;
        for (var v : variants) {
            weight += v.weight;
        }

        final var selector = rand.nextInt(weight);
        weight = 0;
        for (var v : variants) {
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

    record Case(ObjectList<Variant> apply, BlockStateDef.Case.Condition when) {

        interface Condition {
            Condition TRUE = (Map<String, String> state) -> true;

            boolean matches(Map<String, String> state);
        }

        @Desugar
        record MultiCon(boolean requireAll, ObjectList<Condition> matches) implements Condition {
            @Override
            public boolean matches(Map<String, String> state) {
                if (requireAll) {
                    for (var m : matches) {
                        if (!m.matches(state)) return false;
                    }
                    return true;
                }

                for (var m : matches) {
                    if (m.matches(state)) return true;
                }
                return false;
            }
        }

        @Desugar
        record StateCon(String key, ObjectList<String> values) implements Condition {
            @Override
            public boolean matches(Map<String, String> state) {
                final var val = state.get(key);
                for (var v : values) {
                    if (v.equals(val)) return true;
                }
                return false;
            }
        }
    }
}
