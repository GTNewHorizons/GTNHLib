package com.gtnewhorizon.gtnhlib.client.model.baked;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.Condition;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class MultipartModel implements BakedModel {

    private final List<Pair<Condition, BakedModel>> piles;

    public MultipartModel(List<Pair<Condition, BakedModel>> piles) {
        this.piles = piles;
    }

    @Override
    public List<ModelQuadView> getQuads(BakedModelQuadContext context) {
        final var quads = new ObjectArrayList<ModelQuadView>();
        Map<String, String> map = context.getBlockState().toMap();

        for (Pair<Condition, BakedModel> e : piles) {
            if (e.left().matches(map)) {
                quads.addAll(e.right().getQuads(context));
            }
        }
        return quads;
    }

    @Override
    public Position.ModelDisplay getDisplay(Position pos, BakedModelQuadContext context) {
        Map<String, String> map = context.getBlockState().toMap();

        for (Pair<Condition, BakedModel> entry : piles) {
            if (entry.left().matches(map)) {
                return entry.right().getDisplay(pos, context);
            }
        }
        return Position.ModelDisplay.DEFAULT;
    }

    @Override
    public IIcon getParticle(BakedModelQuadContext context) {
        Map<String, String> map = context.getBlockState().toMap();

        for (Pair<Condition, BakedModel> entry : piles) {
            if (entry.left().matches(map)) {
                return entry.right().getParticle(context);
            }
        }

        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("missingno");
    }
}
