package com.gtnewhorizon.gtnhlib.client.model;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackY;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackZ;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.DIRECTIONS;
import static java.lang.Math.max;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.state.BlockState;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

@ThreadSafeISBRH(perThread = true)
public class ModelISBRH implements ISimpleBlockRenderingHandler {

    /**
     * Any blocks using a JSON model should return this for {@link Block#getRenderType()}.
     */
    public static final int JSON_ISBRH_ID = RenderingRegistry.getNextAvailableRenderId();

    private final Random RAND = new Random();

    public ModelISBRH() {}

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
        final var random = world instanceof World worldIn ? worldIn.rand : RAND;
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

                if (quad.getColorIndex() != -1 && color == -1) {
                    color = block.colorMultiplier(world, x, y, z);
                }

                final float r = (color & 255) / 255f;
                final float g = (color >> 8 & 255) / 255f;
                final float b = (color >> 16 & 255) / 255f;

                final int lm = getLightMap(block, quad, world, x, y, z);
                tesselator.setBrightness(lm);

                final float shade = diffuseLight(quad.getComputedFaceNormal());
                tesselator.setColorOpaque_F(r * shade, g * shade, b * shade);
                renderQuad(quad, x, y, z, tesselator, renderer.overrideBlockTexture);
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
                    getUV(quad.getTexU(i), overrideIcon, i, true),
                    getUV(quad.getTexV(i), overrideIcon, i, false));
        }
    }

    private double getUV(double defauld, @Nullable IIcon override, int idx, boolean U) {
        if (override == null) return defauld;

        var ret = 0.0;
        if (U) {
            ret = switch (idx) {
                case 0, 3 -> override.getMinU();
                case 1, 2 -> override.getMaxU();
                default -> throw new IllegalArgumentException("index out of bounds!");
            };
            return ret;
        }

        ret = switch (idx) {
            case 0, 1 -> override.getMinV();
            case 2, 3 -> override.getMaxV();
            default -> throw new IllegalArgumentException("index out of bounds!");
        };
        return ret;
    }

    private int getLightMap(Block block, ModelQuadView quad, IBlockAccess world, int x, int y, int z) {
        final var dir = quad.getLightFace();

        // If the face is aligned or external, pick light outside
        final float avgPos = getAveragePos(quad, dir);
        final var useOuterLight = switch (dir) {
            case POS_X, POS_Y, POS_Z -> avgPos >= 1.0;
            case NEG_X, NEG_Y, NEG_Z -> avgPos <= 0.0;
            case UNASSIGNED -> throw new AssertionError("Light face should never be unassigned!");
        };
        if (useOuterLight) {
            final int lx = x + dir.getStepX();
            final int ly = y + dir.getStepY();
            final int lz = z + dir.getStepZ();
            return block.getMixedBrightnessForBlock(world, lx, ly, lz);
        }

        // The face is inset to some degree, pick self light (if transparent)
        if (block.getLightOpacity(world, x, y, z) != 255) {
            return block.getMixedBrightnessForBlock(world, x, y, z);
        }

        // ...or greatest among neighbors otherwise. Skip the ones in the wrong direction, though.
        int lm = block.getMixedBrightnessForBlock(world, x, y, z);;
        for (int i = 0; i < 6; i++) {
            final var neighbor = DIRECTIONS[i];
            if (neighbor == dir.getOpposite()) continue;

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
}
