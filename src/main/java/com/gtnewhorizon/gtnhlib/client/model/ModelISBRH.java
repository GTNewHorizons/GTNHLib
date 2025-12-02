package com.gtnewhorizon.gtnhlib.client.model;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackY;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackZ;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.DIRECTIONS;
import static java.lang.Math.max;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.ENTITY;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.color.BlockColor;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
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

    private final Vector3f rotated = new Vector3f(0f, 0f, 0f);
    private final Vector3f translated = new Vector3f(0f, 0f, 0f);
    private final Vector3f scaled = new Vector3f(1f, 1f, 1f);

    private void applyItemDisplay(BakedModel model, int meta, ItemRenderType type) {

        Position pos =  switch (type) {
            case EQUIPPED -> Position.THIRDPERSON_RIGHTHAND;
            case ENTITY -> Position.GROUND;
            case INVENTORY -> Position.GUI;
            case EQUIPPED_FIRST_PERSON -> Position.FIRSTPERSON_RIGHTHAND;
            default -> Position.GROUND;
        };

        float px = 0.5f;
        float py = 0.5f;
        float pz = 0.5f;

        Position.ModelDisplay display = model.getDisplay(pos, meta, RAND);

        Vector3f r = display.rotation();
        Vector3f t = display.translation();
        Vector3f s = display.scale();

        // BlockBench to Position
        if (type == EQUIPPED) {
            if (t.equals(translated)) {
                GL11.glTranslatef(0f / 16f, 2.5f / 16f, 0f / 16f);
            } else {
                GL11.glTranslatef(-t.z / 16f, t.y / 16f, t.x / 16f);
            }

            GL11.glTranslatef(px, py, pz);
            if (r.equals(rotated)) {
                GL11.glRotatef(75f, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(45f, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(0f, 1.0f, 0.0f, 0.0f);
            } else {
                GL11.glRotatef(r.x, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(r.y, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(-r.z, 1.0f, 0.0f, 0.0f);
            }

            if (s.equals(scaled)) {
                GL11.glScaled(0.375f, 0.375f, 0.375f);
            } else {
                GL11.glScaled(s.z, s.y, s.x);
            }
            GL11.glTranslatef(-px, -py, -pz);
        }

        if (type == EQUIPPED_FIRST_PERSON) {
            if (!t.equals(translated)) {
                GL11.glTranslatef(-t.z / 16f, t.y / 16f, t.x / 16f);
            }

            GL11.glTranslatef(px, py, pz);
            if (r.equals(rotated)) {
                GL11.glRotatef(0f, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(45f, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(0f, 1.0f, 0.0f, 0.0f);
            } else {
                GL11.glRotatef(r.x, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(r.y, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(-r.z, 1.0f, 0.0f, 0.0f);
            }

            if (s.equals(scaled)) {
                GL11.glScaled(0.4f, 0.4f, 0.4f);
            } else {
                GL11.glScaled(s.z, s.y, s.x);
            }
            GL11.glTranslatef(-px, -py, -pz);
        }

        if (type == ENTITY) {
            // Translate to center
            if (t.equals(translated)) {
                GL11.glTranslatef(0f / 16f, 3f / 16f, 0f / 16f);
            } else {
                GL11.glTranslatef(-t.z / 16f, t.y / 16f, t.x / 16f);
            }

            GL11.glTranslatef(px, py, pz);
            if (!r.equals(rotated)) {
                GL11.glRotatef(r.x, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(r.y, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(-r.z, 1.0f, 0.0f, 0.0f);
            }

            if (s.equals(scaled)) {
                GL11.glScaled(0.25f, 0.25f, 0.25f);
            } else {
                GL11.glScaled(s.z, s.y, s.x);
            }
            GL11.glTranslatef(-px, -py, -pz);
        }

        if (type == INVENTORY) {
            if (!t.equals(translated)) {
                GL11.glTranslatef(-t.z / 16f, t.y / 16f, t.x / 16f);
            }

            GL11.glTranslatef(px, py, pz);
            if (r.equals(rotated)) {
                GL11.glRotatef(30f, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(-135f, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(0f, 1.0f, 0.0f, 0.0f);
            } else {
                GL11.glRotatef(r.x, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(r.y, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(-r.z, 1.0f, 0.0f, 0.0f);
            }

            if (s.equals(scaled)) {
                GL11.glScaled(0.625f, 0.625f, 0.625f);
            } else {
                GL11.glScaled(s.z, s.y, s.x);
            }
            GL11.glTranslatef(-px, -py, -pz);
        }
    }

    public IIcon getParticleIcon(Block block, IBlockAccess world, int x, int y, int z, int meta) {
        final var model = getModel(world, block, meta, x, y, z);
        return model.getParticle(meta, RAND);
    }

    public boolean isMissingIcon(IIcon icon) {
        if (icon == null) return false;

        String name = icon.getIconName();
        if (name == null) return false;

        if (name.equals("missingno")) return false;

        return !name.startsWith("MISSING_ICON_BLOCK_");
    }
}
