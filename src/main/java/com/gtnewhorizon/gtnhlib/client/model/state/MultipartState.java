package com.gtnewhorizon.gtnhlib.client.model.state;

import java.util.Map;

import org.jetbrains.annotations.ApiStatus;

import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.MultipartModel;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.Condition;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.MonopartDough;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.UnbakedModel;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class MultipartState implements StateModelMap, UnbakedModel {

    private final ObjectList<Case> multipart;

    MultipartState(ObjectList<Case> multipart) {
        this.multipart = multipart;
    }

    @Override
    public UnbakedModel selectModel(Map<String, String> properties) {
        return this;
    }

    @Override
    public BakedModel bake(BakeData data) {
        final var bread = new Object2ObjectArrayMap<Condition, BakedModel>(multipart.size());
        for (var c : multipart) {
            bread.put(c.when, new MonopartDough(c.apply).bake());
        }

        return new MultipartModel(bread);
    }

    @ApiStatus.Internal
    public static final class Case {

        private final ObjectList<Weighted<JSONVariant>> apply;
        private final Condition when;

        public Case(ObjectList<Weighted<JSONVariant>> apply, Condition when) {
            this.apply = apply;
            this.when = when;
        }

        public ObjectList<Weighted<JSONVariant>> apply() {
            return apply;
        }

        public Condition when() {
            return when;
        }

        public interface Condition {

            Condition TRUE = (Map<String, String> state) -> true;

            boolean matches(Map<String, String> state);
        }

        static final class MultiCon implements Condition {

            private final boolean requireAll;
            private final ObjectList<Condition> matches;

            MultiCon(boolean requireAll, ObjectList<Condition> matches) {
                this.requireAll = requireAll;
                this.matches = matches;
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

            public boolean requireAll() {
                return requireAll;
            }

            public ObjectList<Condition> matches() {
                return matches;
            }
        }

        static final class StateCon implements Condition {

            private final String key;
            private final ObjectList<String> values;

            StateCon(String key, ObjectList<String> values) {
                this.key = key;
                this.values = values;
            }

            @Override
            public boolean matches(Map<String, String> state) {
                final var val = state.get(key);
                for (var v : values) {
                    if (v.equals(val)) return true;
                }
                return false;
            }

            public String key() {
                return key;
            }

            public ObjectList<String> values() {
                return values;
            }
        }
    }
}
