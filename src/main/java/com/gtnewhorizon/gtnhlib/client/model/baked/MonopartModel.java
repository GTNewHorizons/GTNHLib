package com.gtnewhorizon.gtnhlib.client.model.baked;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.model.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.Weighted;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@Desugar
public record MonopartModel(ObjectArrayList<Weighted<BakedModel>> models) implements BakedModel {

    @Override
    public List<ModelQuadView> getQuads(@Nullable IBlockAccess world, int x, int y, int z, Block block, int meta,
                                        ForgeDirection dir, Random random, int color, @Nullable Supplier<ModelQuadViewMutable> quadPool) {
        return Weighted.selectOne(models, random).getQuads(world, x, y, z, block, meta, dir, random, color, quadPool);
    }
}
