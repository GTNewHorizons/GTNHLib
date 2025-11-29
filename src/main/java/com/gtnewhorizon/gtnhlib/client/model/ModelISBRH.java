package com.gtnewhorizon.gtnhlib.client.model;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackY;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackZ;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.DIRECTIONS;
import static java.lang.Math.max;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.ENTITY;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.FIRST_PERSON_MAP;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.color.BlockColor;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.state.BlockState;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

@ThreadSafeISBRH(perThread = true)
public class ModelISBRH implements ISimpleBlockRenderingHandler, IItemRenderer {

    public static final ModelISBRH INSTANCE = new ModelISBRH();

    /**
     * Any blocks using a JSON model should return this for {@link Block#getRenderType()}.
     */
    public static final int JSON_ISBRH_ID = RenderingRegistry.getNextAvailableRenderId();

    private final Random RAND = new Random();

    public ModelISBRH() {}

    /// Override this if you want programmatic model selection
    @SuppressWarnings("unused")
    public BakedModel getModel(IBlockAccess world, Block block, int meta, int x, int y, int z) {
        return ModelRegistry.getBakedModel(new BlockState(block, meta));
    }

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {}

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        final Random random = world instanceof World worldIn ? worldIn.rand : RAND;
        final Tessellator tesselator = TessellatorManager.get();

        // Get the model!
        final int meta = world.getBlockMetadata(x, y, z);
        final var model = getModel(world, block, meta, x, y, z);

        int color = model.getColor(world, x, y, z, block, meta, random);

        var rendered = false;
        for (var dir : DIRECTIONS) {
            // TODO: face culling

            final var quads = model.getQuads(world, x, y, z, block, meta, dir, random, color, null);
            if (quads.isEmpty()) continue;

            // iterates over the quads and dumps em into the tesselator, nothing special
            rendered = true;
            for (final var quad : quads) {
                int quadColor = color;

                // If true use tintIndex color
                if (quad.getColorIndex() != -1) {
                    quadColor = BlockColor.getColor(block, world, x, y, z, meta, quad.getColorIndex());
                }

                final float r = (quadColor & 255) / 255f;
                final float g = (quadColor >> 8 & 255) / 255f;
                final float b = (quadColor >> 16 & 255) / 255f;

                final int lm = getLightMap(block, quad, dir, world, x, y, z, renderer);
                tesselator.setBrightness(lm);

                final float shade = diffuseLight(quad.getComputedFaceNormal());
                tesselator.setColorOpaque_F(r * shade, g * shade, b * shade);
                renderQuad(quad, x, y, z, tesselator, null);
            }
        }

        return rendered;
    }

    public void renderQuad(ModelQuadView quad, float x, float y, float z, Tessellator tessellator,
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

    public int getLightMap(Block block, ModelQuadView quad, ModelQuadFacing dir, IBlockAccess world, int x, int y,
            int z, RenderBlocks rb) {
        // If the face is aligned or external, pick light outside
        final float avgPos = getAveragePos(quad, dir);
        switch (dir) {
            case POS_X, POS_Y, POS_Z -> {
                if (avgPos >= 1.0) {
                    final int lx = x + dir.getStepX();
                    final int ly = y + dir.getStepY();
                    final int lz = z + dir.getStepZ();
                    return block.getMixedBrightnessForBlock(world, lx, ly, lz);
                }
            }
            case NEG_X, NEG_Y, NEG_Z -> {
                if (avgPos <= 0.0) {
                    final int lx = x + dir.getStepX();
                    final int ly = y + dir.getStepY();
                    final int lz = z + dir.getStepZ();
                    return block.getMixedBrightnessForBlock(world, lx, ly, lz);
                }
            }
        }

        // The face is inset to some degree, pick self light (if transparent)
        if (block.getLightOpacity(world, x, y, z) != 0) {
            return block.getMixedBrightnessForBlock(world, x, y, z);
        }

        // ...or greatest among neighbors otherwise
        int lm = block.getMixedBrightnessForBlock(world, x, y, z);;
        for (int i = 0; i < 6; i++) {
            final var neighbor = DIRECTIONS[i];
            final int lx = x + neighbor.getStepX();
            final int ly = y + neighbor.getStepY();
            final int lz = z + neighbor.getStepZ();
            lm = max(lm, block.getMixedBrightnessForBlock(world, lx, ly, lz));
        }

        return lm;
    }

    private float getAveragePos(ModelQuadView quad, ModelQuadFacing dir) {
        float avg = 0;
        for (int i = 0; i < 4; i++) {
            avg += switch (dir.getAxis()) {
                case X -> quad.getX(i);
                case Y -> quad.getY(i);
                case Z -> quad.getZ(i);
            };
        }
        return avg / 4;
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

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null) return;
        int meta = stack.getItemDamage();

        final Tessellator tesselator = TessellatorManager.get();
        final BakedModel model = getModel(null, block, meta, 0, 0, 0);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        tesselator.startDrawingQuads();

        int color = model.getColor(null, 0, 0, 0, block, meta, RAND);

        for (ModelQuadFacing dir : DIRECTIONS) {

            final var quads = model.getQuads(null, 0, 0, 0, block, meta, dir, RAND, color, null);
            if (quads.isEmpty()) {
                continue;
            }

            for (ModelQuadView quad : quads) {
                int quadColor = color;

                // If true use tintIndex color
                if (quad.getColorIndex() != -1) {
                    quadColor = BlockColor.getColor(block, stack, quad.getColorIndex());
                }

                float r = (quadColor & 0xFF) / 255f;
                float g = (quadColor >> 8 & 0xFF) / 255f;
                float b = (quadColor >> 16 & 0xFF) / 255f;

                final float shade = diffuseLight(quad.getComputedFaceNormal());
                tesselator.setColorOpaque_F(r * shade, g * shade, b * shade);
                renderQuad(quad, 0f, 0f, 0f, tesselator, null);
            }
        }

        // Apply ItemBlock BlockBench Display
        applyItemDisplay(model, meta, type);

        tesselator.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void applyItemDisplay(BakedModel model, int meta, ItemRenderType type) {
        // TODO: Use BlockBench display transforms provided in '{}.display'

        // BlockBench to Position
        if (type == EQUIPPED) {
            // Rotated to correct Face
            GL11.glRotatef(180f, 0f, 1f, 0f);
            // Translated to correct position
            GL11.glTranslated(-1f, 0f, -1f);
        }

        if (type == EQUIPPED_FIRST_PERSON || type == FIRST_PERSON_MAP) {
            // Rotated to correct Face
            GL11.glRotatef(90f, 0f, 1f, 0f);
            // Translated to correct position
            GL11.glTranslated(-1f, 0f, 0f);
        }

        if (type == ENTITY) {
            GL11.glTranslated(-0.5f, -0.5f, -0.5f);
        }

        if (type == INVENTORY) {
            // Translated to correct position
            GL11.glTranslated(0f, -0.1f, 0f);
        }
    }

    public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
        Block block = world.getBlock(x, y, z);
        if (block.getRenderType() != JSON_ISBRH_ID) {
            return false;
        }

        final var model = getModel(world, block, meta, x, y, z);

        byte b0 = 4;

        for (int i1 = 0; i1 < b0; ++i1) {
            for (int j1 = 0; j1 < b0; ++j1) {
                for (int k1 = 0; k1 < b0; ++k1) {
                    double d0 = (double) x + ((double) i1 + 0.5D) / (double) b0;
                    double d1 = (double) y + ((double) j1 + 0.5D) / (double) b0;
                    double d2 = (double) z + ((double) k1 + 0.5D) / (double) b0;

                    IIcon particle = model.getParticle(meta, RAND);

                    EntityDiggingFX entity = new EntityDiggingFX(
                            world,
                            d0,
                            d1,
                            d2,
                            d0 - (double) x - 0.5D,
                            d1 - (double) y - 0.5D,
                            d2 - (double) z - 0.5D,
                            block,
                            meta);

                    entity.setParticleIcon(particle);

                    entity.applyColourMultiplier(x, y, z);

                    effectRenderer.addEffect(entity);
                }
            }
        }

        return true;
    }

    public boolean addHitEffects(World worldObj, MovingObjectPosition target, EffectRenderer effectRenderer) {

        Block block = worldObj.getBlock(target.blockX, target.blockY, target.blockZ);

        if (block.getMaterial() != Material.air && block.getRenderType() == JSON_ISBRH_ID) {

            int meta = worldObj.getBlockMetadata(target.blockX, target.blockY, target.blockZ);

            final var model = getModel(worldObj, block, meta, target.blockX, target.blockY, target.blockZ);

            IIcon particle = model.getParticle(meta, RAND);

            float f = 0.1F;
            double d0 = (double) target.blockX
                    + RAND.nextDouble()
                            * (block.getBlockBoundsMaxX() - block.getBlockBoundsMinX() - (double) (f * 2.0F))
                    + (double) f
                    + block.getBlockBoundsMinX();
            double d1 = (double) target.blockY
                    + RAND.nextDouble()
                            * (block.getBlockBoundsMaxY() - block.getBlockBoundsMinY() - (double) (f * 2.0F))
                    + (double) f
                    + block.getBlockBoundsMinY();
            double d2 = (double) target.blockZ
                    + RAND.nextDouble()
                            * (block.getBlockBoundsMaxZ() - block.getBlockBoundsMinZ() - (double) (f * 2.0F))
                    + (double) f
                    + block.getBlockBoundsMinZ();

            if (target.sideHit == 0) {
                d1 = (double) target.blockY + block.getBlockBoundsMinY() - (double) f;
            }

            if (target.sideHit == 1) {
                d1 = (double) target.blockY + block.getBlockBoundsMaxY() + (double) f;
            }

            if (target.sideHit == 2) {
                d2 = (double) target.blockZ + block.getBlockBoundsMinZ() - (double) f;
            }

            if (target.sideHit == 3) {
                d2 = (double) target.blockZ + block.getBlockBoundsMaxZ() + (double) f;
            }

            if (target.sideHit == 4) {
                d0 = (double) target.blockX + block.getBlockBoundsMinX() - (double) f;
            }

            if (target.sideHit == 5) {
                d0 = (double) target.blockX + block.getBlockBoundsMaxX() + (double) f;
            }

            EntityDiggingFX entity = new EntityDiggingFX(worldObj, d0, d1, d2, 0.0D, 0.0D, 0.0D, block, meta);

            entity.applyColourMultiplier(target.blockX, target.blockY, target.blockZ);

            entity.multipleParticleScaleBy(0.6F);

            entity.setParticleIcon(particle);

            effectRenderer.addEffect(entity);

            return true;
        }

        return false;
    }
}
