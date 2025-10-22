package com.gtnewhorizon.gtnhlib.client.model.baked;

import java.util.List;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Desugar
public record MonopartModel(ObjectArrayList<Weighted<BakedModel>> models) implements BakedModel {

    @Override
    public List<ModelQuadView> getQuads(BakedModelQuadContext context) {
        return Weighted.selectOne(models, context.getRandom()).getQuads(context);
    }
}
