package com.gtnewhorizon.gtnhlib.client.model.state;

import java.util.Map;

import org.jetbrains.annotations.ApiStatus;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.UnbakedModel;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.model.baked.MultipartModel;
import com.gtnewhorizon.gtnhlib.client.model.intermodel.MonopartDough;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.Condition;

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

    @Desugar
    @ApiStatus.Internal
    public record Case(ObjectList<Weighted<JSONVariant>> apply, Condition when) {

        public interface Condition {

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
