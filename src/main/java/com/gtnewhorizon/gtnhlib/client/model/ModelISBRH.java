package com.gtnewhorizon.gtnhlib.client.model;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackX;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackY;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8.unpackZ;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.DIRECTIONS;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.VALUES;
import static com.gtnewhorizon.gtnhlib.compat.Mods.ANGELICA;
import static java.lang.Float.intBitsToFloat;
import static java.lang.Math.max;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.ENTITY;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Unique;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.color.BlockColor;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizon.gtnhlib.core.fml.transformers.BlockIconTransformer;
import com.gtnewhorizon.gtnhlib.mixins.early.models.particles.RenderGlobalAccessor;
import com.gtnewhorizon.gtnhlib.mixins.early.models.particles.WorldAccessor;
import com.gtnewhorizon.gtnhlib.util.StdLCG;
import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

@ThreadSafeISBRH(perThread = true)
public class ModelISBRH implements ISimpleBlockRenderingHandler, IItemRenderer {

    public static final ThreadLocal<ModelISBRH> INSTANCE = ThreadLocal.withInitial(ModelISBRH::new);

    /// Any blocks using a JSON model may return this for [Block#getRenderType()]. However, models are primarily
    /// identified via the presence of a blockstate -> model map for the block. If you don't have such a file, you can
    /// implement {@link BlockModelInfo} instead, and simply always return true.
    public static final int JSON_ISBRH_ID = RenderingRegistry.getNextAvailableRenderId();

    private final Random RAND = new StdLCG();

    private final WorldContext worldContext = new WorldContext();
    private final ItemContext itemContext = new ItemContext();

    public ModelISBRH() {}

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {}

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        // Setup for rendering
        final Tessellator tesselator = TessellatorManager.get();
        final IBlockAccess wrappedContext = (world.getBlock(x, y, z) == block) ? world
                : new WorldWrapper(world, block, x, y, z);
        worldContext.set(wrappedContext, x, y, z, RAND);
        final int meta = wrappedContext.getBlockMetadata(x, y, z);

        if (ANGELICA) AngelicaHelper.initAngelicaLighting(tesselator);

        final BakedModel model = ModelRegistry.getBakedModel(worldContext);
        int color = model.getColor(world, x, y, z, block, meta, RAND);

        var rendered = false;
        for (var dir : VALUES) {
            worldContext.quadFacing = dir;
            worldContext.seedRNG(x, y, z);

            final var quads = model.getQuads(worldContext);
            if (quads.isEmpty()) continue;
            if (dir.isDirection() && !renderer.renderAllFaces
                    && !shouldSideBeRendered(
                            world,
                            x + dir.getStepX(),
                            y + dir.getStepY(),
                            z + dir.getStepZ(),
                            dir.toForgeDir().ordinal(),
                            block)) {
                continue;
            }

            // iterates over the quads and dumps em into the tesselator, nothing special
            rendered = true;
            for (final var quad : quads) {
                if (quad.isTransparent() && ForgeHooksClient.getWorldRenderPass() == 0) continue;
                int quadColor = color;

                // If true use tintIndex color
                if (quad.getColorIndex() != -1) {
                    quadColor = BlockColor.getColor(block, world, x, y, z, meta, quad.getColorIndex());
                }

                final float r = (quadColor >> 16 & 255) / 255f;
                final float g = (quadColor >> 8 & 255) / 255f;
                final float b = (quadColor & 255) / 255f;

                final int lm = getLightMap(block, quad, quad.getLightFace(), world, x, y, z, renderer);
                tesselator.setBrightness(lm);

                final float shade;
                if (!ANGELICA) {
                    shade = quad.hasDirectionalShading() ? diffuseLight(quad.getComputedFaceNormal()) : 1;
                } else {
                    AngelicaHelper.quadAngelicaLighting(tesselator, !quad.hasDirectionalShading());
                    shade = 1;
                }

                tesselator.setColorOpaque_F(r * shade, g * shade, b * shade);
                renderQuad(quad, x, y, z, tesselator, renderer.overrideBlockTexture);
            }
        }

        if (ANGELICA) AngelicaHelper.resetAngelicaLighting(tesselator);
        worldContext.reset();
        return rendered;
    }

    public void renderQuad(ModelQuadView quad, float x, float y, float z, Tessellator tessellator,
            @Nullable IIcon overrideIcon) {
        final var overrideTex = overrideIcon != null;
        float u, v;
        for (int i = 0; i < 4; ++i) {
            if (overrideTex) {
                final long pUV = getOverrideUV(quad, i);
                u = intBitsToFloat((int) (pUV >> 32));
                v = intBitsToFloat((int) pUV);
            } else {
                u = quad.getTexU(i);
                v = quad.getTexV(i);
            }

            tessellator.addVertexWithUV(quad.getX(i) + x, quad.getY(i) + y, quad.getZ(i) + z, u, v);
        }
    }

    /// This returns u,v packed in a long bitwise, because Java doesn't have tuple returns. This isn't for performance,
    /// just clarity so I can set u and v in the same function.
    private long getOverrideUV(ModelQuadView quad, int idx) {
        ModelQuadFacing side = quad.getNormalFace();
        float x = quad.getX(idx);
        float y = quad.getY(idx);
        float z = quad.getZ(idx);

        if (side == ModelQuadFacing.UNASSIGNED) {
            int packedNormal = quad.getComputedFaceNormal();
            float nx = NormI8.unpackX(packedNormal);
            float ny = NormI8.unpackY(packedNormal);
            float nz = NormI8.unpackZ(packedNormal);

            float ax = Math.abs(nx);
            float ay = Math.abs(ny);
            float az = Math.abs(nz);

            if (ay >= ax && ay >= az) {
                side = (ny > 0) ? ModelQuadFacing.POS_Y : ModelQuadFacing.NEG_Y;
            } else if (az >= ax && az >= ay) {
                side = (nz > 0) ? ModelQuadFacing.POS_Z : ModelQuadFacing.NEG_Z;
            } else {
                side = (nx > 0) ? ModelQuadFacing.POS_X : ModelQuadFacing.NEG_X;
            }
        }

        float u, v;
        switch (side) {
            case POS_X -> { // EAST
                u = 1.0f - z;
                v = y;
            }
            case NEG_X -> { // WEST
                u = z;
                v = y;
            }
            case POS_Y -> { // UP
                u = x;
                v = z;
            }
            case NEG_Y -> { // DOWN
                u = x;
                v = 1.0f - z;
            }
            case POS_Z -> { // SOUTH
                u = x;
                v = y;
            }
            case NEG_Z -> { // NORTH
                u = 1.0f - x;
                v = y;
            }
            default -> { // Fallback
                u = x;
                v = y;
            }
        }

        return ((long) Float.floatToIntBits(u) << 32) | (Float.floatToIntBits(v) & 0xFFFFFFFFL);
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
                    return getBrightness(block, quad, world, lx, ly, lz);
                }
            }
            case NEG_X, NEG_Y, NEG_Z -> {
                if (avgPos <= 0.0) {
                    final int lx = x + dir.getStepX();
                    final int ly = y + dir.getStepY();
                    final int lz = z + dir.getStepZ();
                    return getBrightness(block, quad, world, lx, ly, lz);
                }
            }
        }

        // The face is inset to some degree, pick self light (if transparent)
        if (block.getLightOpacity(world, x, y, z) < 255) {
            return getBrightness(block, quad, world, x, y, z);
        }

        // ...or greatest among neighbors otherwise
        int lm = getBrightness(block, quad, world, x, y, z);
        for (int i = 0; i < 6; i++) {
            final var neighbor = DIRECTIONS[i];
            final int lx = x + neighbor.getStepX();
            final int ly = y + neighbor.getStepY();
            final int lz = z + neighbor.getStepZ();
            lm = max(lm, getBrightness(block, quad, world, lx, ly, lz));
        }

        return lm;
    }

    private int getBrightness(Block block, ModelQuadView quad, IBlockAccess world, int x, int y, int z) {
        int brightness = block.getMixedBrightnessForBlock(world, x, y, z);
        if (quad.getEmissiveness() > 0) {
            int blockLight = Math.max(quad.getEmissiveness(), (brightness & 0xF0) >> 4);
            int skyLight = Math.max(quad.getEmissiveness(), (brightness & 0xF00000) >> 20);
            return skyLight << 20 | blockLight << 4;
        }
        return brightness;
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
        itemContext.set(stack, RAND);

        final BakedModel model = ModelRegistry.getBakedModel(itemContext);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        tesselator.startDrawingQuads();

        int color = model.getColor(null, 0, 0, 0, block, meta, RAND);

        for (ModelQuadFacing dir : VALUES) {
            // I don't *like* reseeding the RNG before each quad get, but it seems like the only way to ensure a
            // consistent RNG state for each side.
            itemContext.random.setSeed(0);
            itemContext.quadFacing = dir;

            final var quads = model.getQuads(itemContext);
            if (quads.isEmpty()) continue;

            for (ModelQuadView quad : quads) {
                int quadColor = color;

                // If true use tintIndex color
                if (quad.getColorIndex() != -1) {
                    quadColor = BlockColor.getColor(block, stack, quad.getColorIndex());
                }

                final float r = (quadColor >> 16 & 255) / 255f;
                final float g = (quadColor >> 8 & 255) / 255f;
                final float b = (quadColor & 255) / 255f;

                final float shade = diffuseLight(quad.getComputedFaceNormal());
                tesselator.setColorOpaque_F(r * shade, g * shade, b * shade);
                renderQuad(quad, 0f, 0f, 0f, tesselator, null);
            }
        }

        // Apply ItemBlock BlockBench Display
        applyItemDisplay(model, type);

        tesselator.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        itemContext.reset();
    }

    /// TODO: We need to find a good way to make it so the bound fields are not accounted for but I still want to use
    /// [Block#shouldSideBeRendered]. This is so blocks can define custom culling behavior when needed, but the
    /// bound fields are not relevant here because JSON models don't listen to them.
    protected boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side, Block block) {
        return block.shouldSideBeRendered(world, x, y, z, side);
    }

    private static final Vector3f rotated = new Vector3f(0f, 0f, 0f);
    private static final Vector3f translated = new Vector3f(0f, 0f, 0f);
    private static final Vector3f scaled = new Vector3f(1f, 1f, 1f);

    private void applyItemDisplay(BakedModel model, ItemRenderType type) {

        Position pos = switch (type) {
            case EQUIPPED -> Position.THIRDPERSON_RIGHTHAND;
            case ENTITY -> Position.GROUND;
            case INVENTORY -> Position.GUI;
            case EQUIPPED_FIRST_PERSON -> Position.FIRSTPERSON_RIGHTHAND;
            default -> Position.GROUND;
        };

        float px = 0.5f;
        float py = 0.5f;
        float pz = 0.5f;

        Position.ModelDisplay display = model.getDisplay(pos, itemContext);

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

    /// Mirrors the default {@link Block#getIcon(IBlockAccess, int, int, int, int)}, with the exception that the block
    /// is also re-fetched. This is because the blockstate construction does world access anyway, overriding just the
    /// block is annoying and seems less correct.
    ///
    /// Used in {@link BlockIconTransformer}
    public @NotNull IIcon getParticleIcon(@Nullable IBlockAccess world, int x, int y, int z) {
        worldContext.world = world;
        worldContext.x = x;
        worldContext.y = y;
        worldContext.z = z;
        worldContext.random = RAND;
        worldContext.blockState = BlockPropertyRegistry.getBlockState(world, x, y, z);
        final var model = ModelRegistry.getBakedModel(worldContext);
        final var ret = model.getParticle(worldContext);

        worldContext.reset();
        return ret;
    }

    /// Used in {@link BlockIconTransformer}
    @SuppressWarnings("unused")
    public @NotNull IIcon getMissingIcon() {
        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("missingno");
    }

    /// Spawn a particle for an entity walking on a modeled block. This method effectively replaces
    /// [World#spawnParticle(String, double, double, double, double, double, double)] so we can inject a new texture,
    /// using the block position that normally gets dropped before the particle is actually spawned.
    ///
    /// @param rg The RenderGlobal instance the particle is spawned in.
    /// @param blockX X position of the block the particle is spawned from.
    /// @param blockY Y position...
    /// @param blockZ Z position...
    /// @param x X position of the particle
    /// @param y Y position...
    /// @param z Z position...
    /// @param vX X velocity of particle
    /// @param vY Y velocity...
    /// @param vZ Z velocity...
    @Unique
    public static void spawnParticle(RenderGlobal rg, int blockX, int blockY, int blockZ, double x, double y, double z,
            double vX, double vY, double vZ) {
        var mc = Minecraft.getMinecraft();
        if (mc == null || mc.renderViewEntity == null || mc.effectRenderer == null) return;

        // Skip 3/4 of particles when they're set to decreased
        var world = ((RenderGlobalAccessor) rg).getTheWorld();
        if (mc.gameSettings.particleSetting == 1 && world.rand.nextInt(3) == 0) return;

        // Don't render distant (>16 blocks) particles.
        var dx = mc.renderViewEntity.posX - x;
        var dy = mc.renderViewEntity.posY - y;
        var dz = mc.renderViewEntity.posZ - z;
        if (dx * dx + dy * dy * dz * dz > 16 * 16) return;

        var block = world.getBlock(blockX, blockY, blockZ);
        var meta = world.getBlockMetadata(blockX, blockY, blockZ);
        var digFX = new EntityDiggingFX(world, x, y, z, vX, vY, vZ, block, meta);
        digFX.setParticleIcon(INSTANCE.get().getParticleIcon(world, blockX, blockY, blockZ));
        mc.effectRenderer.addEffect(digFX);
    }

    /// A helper to call [#spawnParticle] the same way vanilla calls [World#spawnParticle]. It's *probably* fine to only
    /// call this on the main render global, but "probably" doesn't stop breakages.
    ///
    /// Neither does being careful, but such is life.
    public static void spawnParticleCommon(World world, int blockX, int blockY, int blockZ, double x, double y,
            double z, double vX, double vY, double vZ) {
        for (var iwa : ((WorldAccessor) world).getWorldAccesses()) {
            if (iwa instanceof RenderGlobal rg)
                ModelISBRH.spawnParticle(rg, blockX, blockY, blockZ, x, y, z, vX, vY, vZ);
        }
    }

    /// Some blocks, like LittleTiles, rely on recursively rendering other blocks in the same location. Unfortunately,
    /// this means we have to actually respect the block passed into [ModelISBRH#renderWorldBlock]. In order to read a
    /// blockstate with that, we wrap the original IBA with one that just returns a slightly different block.
    @Desugar
    private record WorldWrapper(IBlockAccess wrapped, Block replace, int x, int y, int z) implements IBlockAccess {

        @Override
        public Block getBlock(int x, int y, int z) {
            if (x == this.x && y == this.y && z == this.z) return replace;
            return wrapped.getBlock(x, y, z);
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return wrapped.getTileEntity(x, y, z);
        }

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int defauld) {
            return wrapped.getLightBrightnessForSkyBlocks(x, y, z, defauld);
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return wrapped.getBlockMetadata(x, y, z);
        }

        /// WARNING: This may improperly ignore the replacing block!
        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int directionIn) {
            return wrapped.isBlockProvidingPowerTo(x, y, z, directionIn);
        }

        /// WARNING: This may improperly ignore the replacing block!
        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return wrapped.isAirBlock(x, y, z);
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return wrapped.getBiomeGenForCoords(x, z);
        }

        @Override
        public int getHeight() {
            return wrapped.getHeight();
        }

        @Override
        public boolean extendedLevelsInChunkCache() {
            return wrapped.extendedLevelsInChunkCache();
        }

        /// WARNING: may improperly ignore replaced block!
        @Override
        public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defauld) {
            return wrapped.isSideSolid(x, y, z, side, defauld);
        }
    }
}
