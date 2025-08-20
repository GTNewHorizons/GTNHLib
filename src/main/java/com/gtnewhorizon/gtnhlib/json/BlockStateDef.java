package com.gtnewhorizon.gtnhlib.json;

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

    static class Case {
        private final ObjectList<Variant> apply;
        private final Condition when;

        Case(ObjectList<Variant> apply, Condition when) {
            this.apply = apply;
            this.when = when;
        }

        interface Condition {
            Condition TRUE = (Map<String, String> state) -> true;
            boolean matches(Map<String, String> state);
        }

        static class MultiCon implements Condition {
            private final boolean requireAll;
            private final ObjectList<Condition> matches;

            MultiCon(boolean requireAll, ObjectList<Condition> matches) {
                this.requireAll = requireAll;
                this.matches = ObjectLists.unmodifiable(matches);
            }

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

        static class StateCon implements Condition {
            final String key;
            final ObjectList<String> values;

            StateCon(String key, ObjectList<String> values) {
                this.key = key;
                this.values = ObjectLists.unmodifiable(values);
            }

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
