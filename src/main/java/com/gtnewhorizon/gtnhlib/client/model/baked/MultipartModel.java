package com.gtnewhorizon.gtnhlib.client.model.baked;

import static it.unimi.dsi.fastutil.objects.Object2ObjectMaps.unmodifiable;

import java.util.List;
import java.util.Map;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.Condition;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps.UnmodifiableMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Desugar
public record MultipartModel(UnmodifiableMap<Condition, BakedModel> piles) implements BakedModel {

    public MultipartModel(Object2ObjectArrayMap<Condition, BakedModel> piles) {
        this((UnmodifiableMap<Condition, BakedModel>) unmodifiable(piles));
    }

    @Override
    public List<ModelQuadView> getQuads(BakedModelQuadContext context) {
        final var quads = new ObjectArrayList<ModelQuadView>();

        Map<String, String> map = context.getBlockState().toMap();

        Object2ObjectMaps.fastForEach(piles, e -> {
            if (e.getKey().matches(map)) {
                quads.addAll(e.getValue().getQuads(context));
            }
        });

        return quads;
    }
}
