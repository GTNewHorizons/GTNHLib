package com.gtnewhorizon.gtnhlib.client.model.baked;

import com.gtnewhorizon.gtnhlib.client.model.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.state.MultipartState.Case.Condition;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import org.jetbrains.annotations.Nullable;

public record MultipartModel(Object2ObjectMap<Condition, BakedModel> piles)
    implements BakedModel {

    @Override
    public List<QuadView> getQuads(@Nullable IBlockAccess world, int x, int y, int z, Block block, int meta, ForgeDirection dir, Random random, int color, @Nullable Supplier<QuadView> quadPool) {
        final var quads = new ObjectArrayList<QuadView>();
        Object2ObjectMaps.fastForEach(piles, e -> {
            if (e.getKey().matches(stateMap(meta))) quads.addAll(e.getValue().getQuads(world, x, y, z, block, meta, dir, random, color, quadPool));
        });
        return quads;
    }

    private static Map<String, String> stateMap(int meta) {
        final var m = new HashMap<String, String>();
        m.put("meta", Integer.toString(meta));
        return m;
    }
}
