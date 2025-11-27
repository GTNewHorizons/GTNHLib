package com.gtnewhorizon.gtnhlib.client.model.baked;

import static it.unimi.dsi.fastutil.objects.Object2ObjectMaps.unmodifiable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.Condition;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

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
    public List<ModelQuadView> getQuads(@Nullable IBlockAccess world, int x, int y, int z, Block block, int meta,
            ModelQuadFacing dir, Random random, int color, @Nullable Supplier<ModelQuadViewMutable> quadPool) {
        final var quads = new ObjectArrayList<ModelQuadView>();
        Object2ObjectMaps.fastForEach(piles, e -> {
            if (e.getKey().matches(stateMap(meta)))
                quads.addAll(e.getValue().getQuads(world, x, y, z, block, meta, dir, random, color, quadPool));
        });
        return quads;
    }

    @Override
    public Position.ModelDisplay getDisplay(Position pos, int meta, Random random) {
        for (Map.Entry<Condition, BakedModel> entry : piles.entrySet()) {
            if (entry.getKey().matches(stateMap(meta))) {
                return entry.getValue().getDisplay(pos, meta, random);
            }
        }
        return Position.ModelDisplay.DEFAULT;
    }

    private static Map<String, String> stateMap(int meta) {
        final var m = new HashMap<String, String>();
        m.put("meta", Integer.toString(meta));
        return m;
    }
}
