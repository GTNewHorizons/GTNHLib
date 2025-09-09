package com.gtnewhorizon.gtnhlib.client.model;

import com.gtnewhorizon.gtnhlib.block.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.util.DirectionUtil;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.jetbrains.annotations.Nullable;

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
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        final Random random = world instanceof World worldIn ? worldIn.rand : RAND;
        final Tessellator tesselator = Tessellator.instance;
        final Supplier<QuadView> sq = null;

        // Get the model!
        final int meta = world.getBlockMetadata(x, y, z);
        final var model = getModel(world, block, meta, x, y, z);

        int color = model.getColor(world, x, y, z, block, meta, random);

        tesselator.setBrightness(
            world instanceof World worldIn
                ? worldIn.getBlockLightValue_do(x, y, z, block.getUseNeighborBrightness())
                : block.getMixedBrightnessForBlock(world, x, y, z));

        var rendered = false;
        for (ForgeDirection dir : DirectionUtil.ALL_DIRECTIONS) {
            // TODO: face culling

            final var quads = model.getQuads(world, x, y, z, block, meta, dir, random, color, sq);
            if (quads.isEmpty()) continue;

            // iterates over the quads and dumps em into the tesselator, nothing special
            rendered = true;
            for (final QuadView quad : quads) {

                if (quad.getColorIndex() != -1 && color == -1) {
                    color = block.colorMultiplier(world, x, y, z);
                }

                final int r = color & 255;
                final int g = color >> 8 & 255;
                final int b = color >> 16 & 255;

                // TODO: look into 21.5+ model-based AO and the NeoForge light pipeline

                tesselator.setColorOpaque(r, g, b);
                renderQuad(quad, x, y, z, tesselator, null);
            }
        }

        return rendered;
    }

    protected void renderQuad(QuadView quad, float x, float y, float z, Tessellator tessellator, @Nullable IIcon overrideIcon) {
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
}
