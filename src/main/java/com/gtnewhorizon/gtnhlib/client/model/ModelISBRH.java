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
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.state.BlockState;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizon.gtnhlib.color.ImmutableColor;
import com.gtnewhorizon.gtnhlib.color.RGBColor;
import com.gtnewhorizon.gtnhlib.util.ObjectPooler;
import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

@ThreadSafeISBRH(perThread = true)
public class ModelISBRH implements ISimpleBlockRenderingHandler {

    /**
     * Any blocks using a JSON model should return this for {@link Block#getRenderType()}.
     */
    public static final int JSON_ISBRH_ID = RenderingRegistry.getNextAvailableRenderId();

    private final Random random = new Random();

    private final WorldContext worldContext = new WorldContext();
    private final ItemContext itemContext = new ItemContext();

    private final ObjectPooler<ModelQuadViewMutable> quadPool = new ObjectPooler<>(ModelQuad::new);

    public ModelISBRH() {}

    @Override
    public int getRenderId() {
        return JSON_ISBRH_ID;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    /// Override this if you want programmatic model selection
    /// [BakedModelQuadContext#getQuadFacing()] and [BakedModelQuadContext#getColor()()] will return null here.
    protected BakedModel getModel(BakedModelQuadContext context) {
        return ModelRegistry.getBakedModel(context.getBlockState());
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glRotatef(90f, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        Tessellator tessellator = TessellatorManager.get();

        itemContext.stack = new ItemStack(block, metadata);
        itemContext.blockState = BlockPropertyRegistry.getBlockState(itemContext.stack);
        itemContext.random = random;

        // Get the model!
        final BakedModel model = getModel(itemContext);

        itemContext.color = model.getColor(itemContext);

        boolean dynamic = model.isDynamic();

        if (dynamic) {
            itemContext.quadPool = quadPool::getInstance;
        }

        tessellator.startDrawingQuads();

        for (var dir : DIRECTIONS) {
            itemContext.quadFacing = dir;

            final var quads = model.getQuads(itemContext);

            if (quads.isEmpty()) continue;

            // iterates over the quads and dumps em into the tessellator, nothing special
            for (final var quad : quads) {
                // idk where you get the tints from :caught:
                ImmutableColor color = quad.getColorIndex() != -1 ? RGBColor.WHITE : itemContext.color;

                final float r = color.getRed() / 255f;
                final float g = color.getGreen() / 255f;
                final float b = color.getBlue() / 255f;

                tessellator.setBrightness(15);

                tessellator.setColorOpaque_F(r, g, b);
                renderQuad(quad, 0, 0, 0, tessellator, null);

                if (dynamic && quad instanceof ModelQuadViewMutable mqvm) {
                    quadPool.releaseInstance(mqvm.reset());
                }
            }
        }

        tessellator.draw();

        itemContext.reset();

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        final Random random = world instanceof World worldIn ? worldIn.rand : this.random;
        final Tessellator tessellator = TessellatorManager.get();

        worldContext.world = world;
        worldContext.x = x;
        worldContext.y = y;
        worldContext.z = z;
        worldContext.blockState = BlockPropertyRegistry.getBlockState(world, x, y, z);
        worldContext.random = random;

        // Get the model!
        final BakedModel model = getModel(worldContext);

        worldContext.color = model.getColor(worldContext);

        boolean dynamic = model.isDynamic();

        if (dynamic) {
            itemContext.quadPool = quadPool::getInstance;
        }

        var rendered = false;
        for (var dir : DIRECTIONS) {
            worldContext.quadFacing = dir;

            // TODO: face culling

            final var quads = model.getQuads(worldContext);

            if (quads.isEmpty()) continue;

            // iterates over the quads and dumps em into the tessellator, nothing special
            rendered = true;

            for (final var quad : quads) {
                // idk where you get the tints from :caught:
                ImmutableColor color = quad.getColorIndex() != -1 ? RGBColor.WHITE : worldContext.color;

                final float r = color.getRed() / 255f;
                final float g = color.getGreen() / 255f;
                final float b = color.getBlue() / 255f;

                final int lx = x + dir.getStepX();
                final int ly = y + dir.getStepY();
                final int lz = z + dir.getStepZ();
                final int lm = block.getMixedBrightnessForBlock(world, lx, ly, lz);
                tessellator.setBrightness(lm);

                final float shade = diffuseLight(quad.getComputedFaceNormal());
                tessellator.setColorOpaque_F(r * shade, g * shade, b * shade);
                renderQuad(quad, x, y, z, tessellator, null);

                if (dynamic && quad instanceof ModelQuadViewMutable mqvm) {
                    quadPool.releaseInstance(mqvm.reset());
                }
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

    public static float diffuseLight(int normal) {
        final var nx = unpackX(normal);
        final var ny = unpackY(normal);
        final var nz = unpackZ(normal);
        return Math.min(nx * nx * 0.6F + ny * ny * ((3.0F + ny) / 4.0F) + nz * nz * 0.8F, 1.0F);
    }

    private static class WorldContext implements BakedModelQuadContext.World {

        public IBlockAccess world;
        public int x, y, z;
        public BlockState blockState;
        public ModelQuadFacing quadFacing;
        public Random random;
        public ImmutableColor color;
        public Supplier<ModelQuadViewMutable> quadPool;

        public void reset() {
            this.world = null;
            if (blockState != null) blockState.close();
            this.blockState = null;
            this.quadFacing = null;
            this.random = null;
            this.color = null;
            this.quadPool = null;
        }

        @Override
        public IBlockAccess getWorld() {
            return world;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getZ() {
            return z;
        }

        @Override
        public BlockState getBlockState() {
            return blockState;
        }

        @Override
        public ModelQuadFacing getQuadFacing() {
            return quadFacing;
        }

        @Override
        public Random getRandom() {
            return random;
        }

        @Override
        public ImmutableColor getColor() {
            return color;
        }

        @Override
        public @Nullable Supplier<ModelQuadViewMutable> getQuadPool() {
            return quadPool;
        }
    }

    private static class ItemContext implements BakedModelQuadContext.Item {

        public ItemStack stack;
        public BlockState blockState;
        public ModelQuadFacing quadFacing;
        public Random random;
        public ImmutableColor color;
        public Supplier<ModelQuadViewMutable> quadPool;

        public void reset() {
            stack = null;
            if (blockState != null) blockState.close();
            blockState = null;
            quadFacing = null;
            random = null;
            color = null;
            quadPool = null;
        }

        @Override
        public ItemStack getItemStack() {
            return stack;
        }

        @Override
        public BlockState getBlockState() {
            return blockState;
        }

        @Override
        public ModelQuadFacing getQuadFacing() {
            return quadFacing;
        }

        @Override
        public Random getRandom() {
            return random;
        }

        @Override
        public ImmutableColor getColor() {
            return color;
        }

        @Override
        public @Nullable Supplier<ModelQuadViewMutable> getQuadPool() {
            return quadPool;
        }
    }
}
