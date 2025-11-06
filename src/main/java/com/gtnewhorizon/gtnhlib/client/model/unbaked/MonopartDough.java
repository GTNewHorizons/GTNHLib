package com.gtnewhorizon.gtnhlib.client.model.unbaked;

import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.MonopartModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public final class MonopartDough implements UnbakedModel {

    private final ObjectList<Weighted<JSONVariant>> variants;

    public MonopartDough(ObjectList<Weighted<JSONVariant>> variants) {
        this.variants = variants;
    }

    @Override
    public BakedModel bake(BakeData data) {
        // Load and bake the models
        var models = new ObjectArrayList<Weighted<BakedModel>>(variants.size());
        for (var v : variants) {
            final var model = ModelRegistry.getJSONModel(v.thing().model());
            final var bread = model.bake(v.thing());
            models.add(new Weighted<>(bread, v.weight()));
        }

        if (models.size() == 1) return models.get(0).thing();
        return new MonopartModel(models);
    }

    public ObjectList<Weighted<JSONVariant>> variants() {
        return variants;
    }
}
