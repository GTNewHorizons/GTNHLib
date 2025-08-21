package com.gtnewhorizon.gtnhlib.json;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.Variant;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.List;
import java.util.Map;

public class BlockStateDef {
    private final Object2ObjectMap<StateMatch, List<Variant>> variants;
    private final ObjectList<Case> multipart;

    BlockStateDef(Object2ObjectMap<StateMatch, List<Variant>> variants) {
        this.variants = Object2ObjectMaps.unmodifiable(variants);
        multipart = null;
    }

    BlockStateDef(ObjectList<Case> multipart) {
        variants = null;
        this.multipart = ObjectLists.unmodifiable(multipart);
    }

    static class StateMatch {
        private final boolean matchAll;
        private final Map<String, String> states;

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
