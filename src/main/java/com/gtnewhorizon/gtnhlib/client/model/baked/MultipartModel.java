package com.gtnewhorizon.gtnhlib.client.model.baked;

import static it.unimi.dsi.fastutil.objects.Object2ObjectMaps.unmodifiable;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.Condition;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps.UnmodifiableMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class MultipartModel implements BakedModel {

    private final UnmodifiableMap<Condition, BakedModel> piles;

    public MultipartModel(UnmodifiableMap<Condition, BakedModel> piles) {
        this.piles = piles;
    }

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

    @Override
    public Position.ModelDisplay getDisplay(Position pos, BakedModelQuadContext context) {
        Map<String, String> map = context.getBlockState().toMap();

        for (Map.Entry<Condition, BakedModel> entry : piles.entrySet()) {
            if (entry.getKey().matches(map)) {
                return entry.getValue().getDisplay(pos, context);
            }
        }
        return Position.ModelDisplay.DEFAULT;
    }

    @Override
    public IIcon getParticle(BakedModelQuadContext context) {
        Map<String, String> map = context.getBlockState().toMap();

        for (Map.Entry<Condition, BakedModel> entry : piles.entrySet()) {
            if (entry.getKey().matches(map)) {
                return entry.getValue().getParticle(context);
            }
        }

        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("missingno");
    }
}
