package com.gtnewhorizon.gtnhlib.client.model;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackY;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackZ;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis.X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis.Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis.Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.DIRECTIONS;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFlags.contains;
import static java.lang.Math.max;
import static java.lang.Math.round;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
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
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFlags;
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
    private final int[] lm = new int[4];
    private final int[] lmScratchPartial = new int[4];
    private final float[] br = new float[4];
    private final float[] brScratchPartial = new float[4];

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
        final var tesselator = TessellatorManager.get();
        final var smoothLight = Minecraft.isAmbientOcclusionEnabled();

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

                if (!smoothLight) {
                    final int lm = getLightMap(block, quad, world, x, y, z);
                    tesselator.setBrightness(lm);
                } else {
                    getLightMapSmooth(block, quad, world, x, y, z);
                }

                renderQuad(quad, x, y, z, tesselator, renderer.overrideBlockTexture, smoothLight, color);
            }
        }

        return rendered;
    }

    protected void renderQuad(ModelQuadView quad, int x, int y, int z, Tessellator tessellator,
            @Nullable IIcon overrideIcon, boolean smoothLight, int color) {
        final float shade = diffuseLight(quad.getComputedFaceNormal());

        final float r = (color & 255) / 255f;
        final float g = (color >> 8 & 255) / 255f;
        final float b = (color >> 16 & 255) / 255f;
        tessellator.setColorOpaque_F(r * shade, g * shade, b * shade);

        for (int i = 0; i < 4; ++i) {
            if (smoothLight) {
                tessellator.setBrightness(lm[i]);
                tessellator.setColorOpaque_F(r * shade * br[i], g * shade * br[i], b * shade * br[i]);
            }

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

        final var useOuterLight = !isQuadInset(quad, dir);
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

    private boolean isQuadInset(ModelQuadView quad, ModelQuadFacing dir) {
        float avg = 0;
        for (int i = 0; i < 4; i++) {
            avg += switch (dir.getAxis()) {
                case X -> quad.getX(i);
                case Y -> quad.getY(i);
                case Z -> quad.getZ(i);
            };
        }
        final float avgPos = avg / 4;

        // If the face is, on average, inside the block (on the given axis), it's inset.
        return switch (dir) {
            case POS_X, POS_Y, POS_Z -> avgPos < 1;
            case NEG_X, NEG_Y, NEG_Z -> avgPos > 0;
            case UNASSIGNED -> throw new AssertionError("Light face should never be unassigned!");
        };
    }

    private void getLightMapSmooth(Block block, ModelQuadView quad, IBlockAccess world, int x, int y, int z) {
        final var dir = quad.getLightFace();

        // Calculate lightmaps for full faces (and inset faces)
        calcLMFull(world, x, y, z, dir, isQuadInset(quad, dir));

        // Adjust if needed
        if (contains(quad.getFlags(), ModelQuadFlags.IS_PARTIAL)) {
            adjustLMPartial(quad, dir);
        }
    }

    private void calcLMFull(IBlockAccess world, int x, int y, int z, ModelQuadFacing dir, boolean inset) {
        // Models have canonical vertex order - top left, top right, bottom right, bottom left.
        for (int i = 0; i < 4; ++i) {
            int blockLight = 0;
            int skyLight = 0;
            float brightness = 0;
            float counter = 0;

            int qx;
            int qy;
            int qz;

            // Average light from surrounding blocks
            qx = x + getOffset(X, 1, i, dir, inset);
            qy = y + getOffset(Y, 1, i, dir, inset);
            qz = z + getOffset(Z, 1, i, dir, inset);
            final var edge0Block = world.getBlock(qx, qy, qz);
            boolean edge0Clear = edge0Block.getLightOpacity(world, qx, qy, qz) < 255;
            brightness += edge0Block.getAmbientOcclusionLightValue();
            if (edge0Clear) {
                ++counter;
                final int lm = edge0Block.getMixedBrightnessForBlock(world, qx, qy, qz);
                skyLight += lm >>> 20;
                blockLight += lm >>> 4 & 0xFF;
            }

            qx = x + getOffset(X, 3, i, dir, inset);
            qy = y + getOffset(Y, 3, i, dir, inset);
            qz = z + getOffset(Z, 3, i, dir, inset);
            final var edge1Block = world.getBlock(qx, qy, qz);
            boolean edge1Clear = edge1Block.getLightOpacity(world, qx, qy, qz) < 255;
            brightness += edge1Block.getAmbientOcclusionLightValue();
            if (edge1Clear) {
                ++counter;
                final int lm = edge1Block.getMixedBrightnessForBlock(world, qx, qy, qz);
                skyLight += lm >>> 20;
                blockLight += lm >>> 4 & 0xFF;
            }

            qx = x + getOffset(X, 0, i, dir, inset);
            qy = y + getOffset(Y, 0, i, dir, inset);
            qz = z + getOffset(Z, 0, i, dir, inset);
            final var cornerBlock = world.getBlock(qx, qy, qz);
            boolean cornerClear = cornerBlock.getLightOpacity(world, qx, qy, qz) < 255;
            // Corner only matters if one of the edges is clear
            if (edge0Clear | edge1Clear) {
                brightness += cornerBlock.getAmbientOcclusionLightValue();
                if (cornerClear) {
                    ++counter;
                    final int lm = cornerBlock.getMixedBrightnessForBlock(world, qx, qy, qz);
                    skyLight += lm >>> 20;
                    blockLight += lm >>> 4 & 0xFF;
                }
            } else {
                // If both edges are full, the corner is effectively full.
                brightness += 0.2f;
            }

            qx = x + getOffset(X, 2, i, dir, inset);
            qy = y + getOffset(Y, 2, i, dir, inset);
            qz = z + getOffset(Z, 2, i, dir, inset);
            final var selfBlock = world.getBlock(qx, qy, qz);
            boolean selfClear = selfBlock.getLightOpacity(world, qx, qy, qz) < 255;
            brightness += selfBlock.getAmbientOcclusionLightValue();
            if (selfClear) {
                ++counter;
                final int lm = selfBlock.getMixedBrightnessForBlock(world, qx, qy, qz);
                skyLight += lm >>> 20;
                blockLight += lm >>> 4 & 0xFF;
            }

            int isky = round(skyLight / counter) << 20;
            int iblock = round(blockLight / counter) << 4;

            lm[i] = isky | iblock;
            br[i] = brightness / 4;
        }
    }

    private void adjustLMPartial(ModelQuadView quad, ModelQuadFacing dir) {
        // Save original values
        System.arraycopy(lm, 0, lmScratchPartial, 0, lm.length);
        System.arraycopy(br, 0, brScratchPartial, 0, br.length);

        for (int i = 0; i < 4; ++i) {
            float left = getLeft(quad, dir, i);
            float top = getTop(quad, dir, i);

            // Bilinearly interpolate weights for the vertex, i.e. get how close is it to the full-face vertices.
            float w0 = left * top;
            float w1 = left * (1 - top);
            float w2 = (1 - left) * (1 - top);
            float w3 = (1 - left) * top;

            // Reassign brightness and lightmaps accordingly
            br[i] = w0 * brScratchPartial[0] + w1 * brScratchPartial[1]
                    + w2 * brScratchPartial[2]
                    + w3 * brScratchPartial[3];
            lm[i] = blendLM(w0, w1, w2, w3);
        }
    }

    private int blendLM(float w0, float w1, float w2, float w3) {
        int sky0 = lm[0] >>> 20;
        int sky1 = lm[1] >>> 20;
        int sky2 = lm[2] >>> 20;
        int sky3 = lm[3] >>> 20;
        int sky = round(sky0 * w0 + sky1 * w1 + sky2 * w2 + sky3 * w3);

        int blk0 = lm[0] >>> 4;
        int blk1 = lm[1] >>> 4;
        int blk2 = lm[2] >>> 4;
        int blk3 = lm[3] >>> 4;
        int blk = round(blk0 * w0 + blk1 * w1 + blk2 * w2 + blk3 * w3);

        return sky << 20 | blk << 4;
    }

    private static float getLeft(ModelQuadView quad, ModelQuadFacing dir, int idx) {
        return switch (dir) {
            case NEG_Y, POS_Y, POS_Z -> 1 - quad.getX(idx);
            case NEG_Z -> quad.getX(idx);
            case NEG_X -> 1 - quad.getZ(idx);
            case POS_X -> quad.getZ(idx);
            case UNASSIGNED -> throw new AssertionError("No offset for unassigned quads!");
        };
    }

    private static float getTop(ModelQuadView quad, ModelQuadFacing dir, int idx) {
        return switch (dir) {
            case NEG_Y -> quad.getZ(idx);
            case POS_Y -> 1 - quad.getZ(idx);
            case NEG_Z, POS_Z, NEG_X, POS_X -> quad.getY(idx);
            case UNASSIGNED -> throw new AssertionError("No offset for unassigned quads!");
        };
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

    /// Return the offset for getting light relative to a given face. The canonical order (used for vertIdx) starts with
    /// the top left vertex for all of these, and thus:
    /// - +X: starts from max, max, max (X, Y, Z)
    /// - -X: starts from min, max, min
    /// - +Y: starts from max, max, max
    /// - -Y: starts from max, min, min
    /// - +Z: starts from min, max, max
    /// - -Z: starts from max, max, min
    private int getOffset(ModelQuadFacing.Axis axis, int offsetIdx, int vertIdx, ModelQuadFacing dir, boolean inset) {
        return switch (dir) {
            // -Y is "out", Z is "top" and -X is "left"
            case NEG_Y -> switch (axis) {
                    case X -> -leftOffset(offsetIdx, vertIdx);
                    case Y -> inset ? 0 : -1;
                    case Z -> topOffset(offsetIdx, vertIdx);
                };
            // +Y is "out", -Z as "top" and -X is "left"
            case POS_Y -> switch (axis) {
                    case X -> -leftOffset(offsetIdx, vertIdx);
                    case Y -> inset ? 0 : 1;
                    case Z -> -topOffset(offsetIdx, vertIdx);
                };
            // -Z is "out", +Y as "top" and +X as "left"
            case NEG_Z -> switch (axis) {
                    case X -> leftOffset(offsetIdx, vertIdx);
                    case Y -> topOffset(offsetIdx, vertIdx);
                    case Z -> inset ? 0 : -1;
                };
            // +Z is "out", +Y as "top" and -X as "left"
            case POS_Z -> switch (axis) {
                    case X -> -leftOffset(offsetIdx, vertIdx);
                    case Y -> topOffset(offsetIdx, vertIdx);
                    case Z -> inset ? 0 : 1;
                };
            // -X is "out", +Y is "top", -Z is "left"
            case NEG_X -> switch (axis) {
                    case X -> inset ? 0 : -1;
                    case Y -> topOffset(offsetIdx, vertIdx);
                    case Z -> -leftOffset(offsetIdx, vertIdx);
                };
            // +X is "out", +Y is "top", +Z is "left"
            case POS_X -> switch (axis) {
                    case X -> inset ? 0 : 1;
                    case Y -> topOffset(offsetIdx, vertIdx);
                    case Z -> leftOffset(offsetIdx, vertIdx);
                };
            case UNASSIGNED -> throw new AssertionError("No offset for unassigned quads!");
        };
    }

    /// The offset order is corner, edge, self, edge, going CCW. For the top left vertex, that'd be top left, left,
    /// self, top.
    ///
    /// Offset table:
    /// | Offset Index | Vertex 0 | Vertex 1 | Vertex 2 | Vertex 3 |
    /// | ------------ | -------- | -------- | -------- | -------- |
    /// | 0 | 1, 1 | -1, 1 | -1,-1 | 1,-1 |
    /// | 1 | 0, 1 | -1, 0 | 0,-1 | 1, 0 |
    /// | 2 | 0, 0 | 0, 0 | 0, 0 | 0, 0 |
    /// | 3 | 1, 0 | 0, 1 | -1, 0 | 0,-1 |
    private static int topOffset(int offsetIdx, int vertIdx) {
        return OFFSETS_TOP[offsetIdx + (vertIdx << 2)];
    }

    private static int leftOffset(int offsetIdx, int vertIdx) {
        return OFFSETS_LEFT[offsetIdx + (vertIdx << 2)];
    }

    private static final int[] OFFSETS_TOP = { 1, 0, 0, 1, // vert 0
            -1, -1, 0, 0, // vert 1
            -1, 0, 0, -1, // vert 2
            1, 1, 0, 0, // vert 3
    };

    private static final int[] OFFSETS_LEFT = { 1, 1, 0, 0, // vert 0
            1, 0, 0, 1, // vert 1
            -1, -1, 0, 0, // vert 2
            -1, 0, 0, -1 // vert 3
    };
}
