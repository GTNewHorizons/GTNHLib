package com.gtnewhorizon.gtnhlib.client.model.baked;

import java.util.List;

import net.minecraft.util.IIcon;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class MonopartModel implements BakedModel {

    private final ObjectArrayList<Weighted<BakedModel>> models;

    public MonopartModel(ObjectArrayList<Weighted<BakedModel>> models) {
        this.models = models;
    }

    @Override
    public List<ModelQuadView> getQuads(BakedModelQuadContext context) {
        return Weighted.selectOne(models, context.getRandom()).getQuads(context);
    }

    @Override
    public Position.ModelDisplay getDisplay(Position pos, BakedModelQuadContext context) {
        return Weighted.selectOne(models, context.getRandom()).getDisplay(pos, context);
    }

    @Override
    public IIcon getParticle(BakedModelQuadContext context) {
        return Weighted.selectOne(models, context.getRandom()).getParticle(context);
    }

    public ObjectArrayList<Weighted<BakedModel>> models() {
        return models;
    }
}
