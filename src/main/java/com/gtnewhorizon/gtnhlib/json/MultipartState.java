package com.gtnewhorizon.gtnhlib.json;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.UnbakedModel;
import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import java.util.Objects;

public class MultipartState implements StateDef {
    private final ObjectList<Case> multipart;

    MultipartState(ObjectList<Case> multipart) {
        this.multipart = multipart;
    }

    @Override
    public UnbakedModel selectModel(Map<String, String> properties) {
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

    record Case(ObjectList<JSONVariant> apply, Condition when) {

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
