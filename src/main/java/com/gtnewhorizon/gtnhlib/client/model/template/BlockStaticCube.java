package com.gtnewhorizon.gtnhlib.client.model.template;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.client.model.ModelLoader;
import com.gtnewhorizon.gtnhlib.client.model.NdQuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.Quad;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadProvider;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;

public class BlockStaticCube implements QuadProvider {

    private final String textureName;
    protected final List<QuadView>[] store = new List[7];

    public BlockStaticCube(String textureName) {
        this.textureName = textureName;
        ModelLoader.registerBaker(this::bake);
    }

    protected void bake() {

        final NdQuadBuilder builder = new NdQuadBuilder();
        for (ForgeDirection f : ForgeDirection.VALID_DIRECTIONS) {

            builder.square(f, 0, 0, 1, 1, 0);
            builder.spriteBake(this.textureName, QuadBuilder.BAKE_LOCK_UV);
            builder.setColors(-1);
            this.store[f.ordinal()] = ObjectImmutableList.of(builder.build(new Quad()));
        }
        this.store[6] = ObjectImmutableList.of();
    }

    @Override
    public List<QuadView> getQuads(IBlockAccess world, int x, int y, int z, Block block, int meta, ForgeDirection dir,
            Random random, int color, Supplier<QuadView> quadPool) {
        return store[dir.ordinal()];
    }
}
