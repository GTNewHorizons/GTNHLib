package com.gtnewhorizon.gtnhlib.client.model.template;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix4f;

import com.gtnewhorizon.gtnhlib.client.model.loading.ModelLoader;
import com.gtnewhorizon.gtnhlib.client.model.loading.NdQuadBuilder;
import com.gtnewhorizon.gtnhlib.client.model.BakedModel;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.Quad;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;

public class ColumnModel implements BakedModel {

    private final String topTex;
    private final String sideTex;
    protected final List<QuadView>[] store = new List[7];
    private boolean rotate = false;
    private Matrix4f rotMat;

    public ColumnModel(String topTex, String sideTex, Matrix4f rotMat) {
        this(topTex, sideTex);
        this.rotate = true;
        this.rotMat = rotMat;
    }

    public ColumnModel(String topTex, String sideTex) {
        this.topTex = topTex;
        this.sideTex = sideTex;
        ModelLoader.registerBaker(this::bake);
    }

    protected void bake() {

        final NdQuadBuilder builder = new NdQuadBuilder();

        for (ForgeDirection f : ForgeDirection.VALID_DIRECTIONS) {

            builder.square(f, 0, 0, 1, 1, 0);

            final String tex = (f == ForgeDirection.UP || f == ForgeDirection.DOWN) ? this.topTex : this.sideTex;
            builder.spriteBake(tex, QuadBuilder.BAKE_LOCK_UV);

            builder.setColors(-1);

            final List<QuadView> tmp = ObjectImmutableList
                    .of((this.rotate) ? builder.build(new Quad(), this.rotMat) : builder.build(new Quad()));

            this.store[tmp.get(0).getCullFace().ordinal()] = tmp;
        }
        this.store[6] = ObjectImmutableList.of();
    }

    @Override
    public List<QuadView> getQuads(IBlockAccess world, int x, int y, int z, Block block, int meta, ForgeDirection dir,
            Random random, int color, Supplier<QuadView> quadPool) {
        return this.store[dir.ordinal()];
    }
}
