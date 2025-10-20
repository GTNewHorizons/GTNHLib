package com.gtnewhorizon.gtnhlib.client.model.baked;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Desugar
public record MonopartModel(ObjectArrayList<Weighted<BakedModel>> models) implements BakedModel {

    @Override
    public List<ModelQuadView> getQuads(@Nullable IBlockAccess world, int x, int y, int z, Block block, int meta,
            ModelQuadFacing dir, Random random, int color, @Nullable Supplier<ModelQuadViewMutable> quadPool) {
        return Weighted.selectOne(models, random).getQuads(world, x, y, z, block, meta, dir, random, color, quadPool);
    }
}
