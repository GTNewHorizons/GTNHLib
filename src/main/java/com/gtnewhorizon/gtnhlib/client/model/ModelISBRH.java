package com.gtnewhorizon.gtnhlib.client.model;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackY;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackZ;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.DIRECTIONS;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.block.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ModelISBRH implements ISimpleBlockRenderingHandler {

    /**
     * Any blocks using a JSON model should return this for {@link Block#getRenderType()}.
     */
    public int JSON_ISBRH_ID = RenderingRegistry.getNextAvailableRenderId();

    public static final ModelISBRH MODEL_ISBRH = new ModelISBRH();
    private static final Random RAND = new Random();

    private ModelISBRH() {}

    /// Override this if you want programmatic model selection
    @SuppressWarnings("unused")
    protected BakedModel getModel(IBlockAccess world, Block block, int meta, int x, int y, int z) {
        return ModelRegistry.getBakedModel(new BlockState(block, meta));
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {

    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        final Random random = world instanceof World worldIn ? worldIn.rand : RAND;
        final Tessellator tesselator = Tessellator.instance;
        final Supplier<ModelQuadViewMutable> sq = null;

        // Get the model!
        final int meta = world.getBlockMetadata(x, y, z);
        final var model = getModel(world, block, meta, x, y, z);

        int color = model.getColor(world, x, y, z, block, meta, random);

        var rendered = false;
        for (var dir : DIRECTIONS) {
            // TODO: face culling

            final var quads = model.getQuads(world, x, y, z, block, meta, dir, random, color, sq);
            if (quads.isEmpty()) continue;

            // iterates over the quads and dumps em into the tesselator, nothing special
            rendered = true;
            for (final ModelQuadView quad : quads) {

                if (quad.getColorIndex() != -1 && color == -1) {
                    color = block.colorMultiplier(world, x, y, z);
                }

                final float r = (color & 255) / 255f;
                final float g = (color >> 8 & 255) / 255f;
                final float b = (color >> 16 & 255) / 255f;

                final int lx = x + dir.getStepX();
                final int ly = y + dir.getStepY();
                final int lz = z + dir.getStepZ();
                final int lm = block.getMixedBrightnessForBlock(world, lx, ly, lz);
                tesselator.setBrightness(lm);

                final float shade = diffuseLight(quad.getComputedFaceNormal());
                tesselator.setColorOpaque_F(r * shade, g * shade, b * shade);
                renderQuad(quad, x, y, z, tesselator, null);
            }
        }

        return rendered;
    }

    protected void renderQuad(ModelQuadView quad, float x, float y, float z, Tessellator tessellator,
            @Nullable IIcon overrideIcon) {
        for (int i = 0; i < 4; ++i) {
            tessellator.addVertexWithUV(
                    quad.getX(i) + x,
                    quad.getY(i) + y,
                    quad.getZ(i) + z,
                    quad.getTexU(i),
                    quad.getTexV(i));
        }
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return JSON_ISBRH_ID;
    }

    public static float diffuseLight(int normal) {
        final var nx = unpackX(normal);
        final var ny = unpackY(normal);
        final var nz = unpackZ(normal);
        return Math.min(nx * nx * 0.6F + ny * ny * ((3.0F + ny) / 4.0F) + nz * nz * 0.8F, 1.0F);
    }
}
