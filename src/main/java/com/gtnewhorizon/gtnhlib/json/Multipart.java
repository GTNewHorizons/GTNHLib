package com.gtnewhorizon.gtnhlib.json;

import java.util.Map;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.UnbakedModel;

import it.unimi.dsi.fastutil.objects.ObjectList;

public class Multipart implements StateModelMap {

    private final ObjectList<Case> multipart;

    Multipart(ObjectList<Case> multipart) {
        this.multipart = multipart;
    }

    @Override
    public UnbakedModel selectModel(Map<String, String> properties) {
        return null;
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
