package com.gtnewhorizon.gtnhlib.client.model.baked;

import com.gtnewhorizon.gtnhlib.client.model.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.NdQuadBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;

public class DynamicCubeModel implements BakedModel {

    /*
     * By the power of ~~theft~~ inspiration, this class is wayyy simpler. It's also a basic example of dynamic model
     * building.
     */

    public static final ThreadLocal<DynamicCubeModel> INSTANCE = ThreadLocal.withInitial(DynamicCubeModel::new);
    private final NdQuadBuilder builder = new NdQuadBuilder();
    private static final List<QuadView> EMPTY = ObjectImmutableList.of();
    private final List<QuadView> ONE = new ObjectArrayList<>(1);

    public DynamicCubeModel() {

        this.ONE.add(null);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public List<QuadView> getQuads(IBlockAccess world, int x, int y, int z, Block block, int meta, ForgeDirection dir,
            Random random, int color, Supplier<QuadView> quadPool) {

        if (dir == ForgeDirection.UNKNOWN) return EMPTY;

        this.builder.square(dir, 0, 0, 1, 1, 0);
        final IIcon tex = block.getIcon(dir.ordinal(), meta);
        this.builder.spriteBake(tex, QuadBuilder.BAKE_LOCK_UV);

        this.builder.setColors(color);

        ONE.set(0, this.builder.build(quadPool.get()));
        return ONE;
    }
}
