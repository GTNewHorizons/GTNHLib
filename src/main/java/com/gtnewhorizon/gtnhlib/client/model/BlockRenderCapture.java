package com.gtnewhorizon.gtnhlib.client.model;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.UNASSIGNED;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.PileOfQuads;
import com.gtnewhorizon.gtnhlib.client.renderer.LocalTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

/**
 * Turns what a block's renderer (vanilla {@link RenderBlocks} or an ISBRH) draws into a {@link BakedModel}, so blocks
 * without JSON models can be used like those with one.
 *
 * <p>
 * The quads are local to the block and grouped by the block face they lie on, quads inside the block are
 * {@link ModelQuadFacing#UNASSIGNED}. Their colors follow the JSON model rules: the vertex color times the block's
 * color multiplier if {@link ModelQuadView#getColorIndex} is 0, times the directional shading if
 * {@link ModelQuadView#hasDirectionalShading}. Vanilla bakes both into the vertex colors, so they're worked out again
 * from {@link Block#colorMultiplier}, quads with colors of their own keep them.
 *
 * <p>
 * Capturing is as thread safe as rendering a chunk: always on the main thread, off it only when the block's bounds are
 * per thread (IE with Angelica). Renderers that touch other shared state (GL, fields on the block) may still break off
 * the main thread, callers should handle exceptions.
 */
public final class BlockRenderCapture {

    /** vanilla rounds the shaded color channels down */
    private static final float COLOR_TOLERANCE = 2.0f;
    private static final float SIDE_EPSILON = 0.0001f;
    private static final int WHITE = -1;

    private BlockRenderCapture() {}

    /** Renders the block once, as it is in the world at the position. */
    public static BakedModel capture(IBlockAccess world, int x, int y, int z, Block block) {
        ArrayList<ModelQuadViewMutable> capturedQuads = new ArrayList<>();
        LocalTessellator tessellator = TessellatorManager.enterLocalMode();
        try {
            tessellator.startDrawingQuads();
            new RenderBlocks(world).renderBlockByRenderType(block, x, y, z);
            tessellator.collectQuads(capturedQuads);
        } finally {
            tessellator.discard();
            TessellatorManager.exitLocalMode();
        }

        Map<ModelQuadFacing, ArrayList<ModelQuadView>> quadsByFacing = new EnumMap<>(ModelQuadFacing.class);
        IIcon particle = null;
        for (ModelQuadViewMutable capturedQuad : capturedQuads) {
            ModelQuad quad = (ModelQuad) capturedQuad;
            for (int vertex = 0; vertex < 4; vertex++) {
                quad.setX(vertex, quad.getX(vertex) - x);
                quad.setY(vertex, quad.getY(vertex) - y);
                quad.setZ(vertex, quad.getZ(vertex) - z);
                // Reset light since we don't want to put that into the model
                quad.setLight(vertex, 0);
            }

            // the tessellator doesn't keep the sprite, the quad's center is inside it on the atlas
            float centerU = (quad.getTexU(0) + quad.getTexU(1) + quad.getTexU(2) + quad.getTexU(3)) / 4.0f;
            float centerV = (quad.getTexV(0) + quad.getTexV(1) + quad.getTexV(2) + quad.getTexV(3)) / 4.0f;
            TextureAtlasSprite sprite = findBlockAtlasSprite(centerU, centerV);
            quad.setSprite(sprite);
            if (particle == null) particle = sprite;

            int colorMultiplier = block.colorMultiplier(world, x, y, z);
            separateColors(quad, colorMultiplier);

            quadsByFacing.computeIfAbsent(getCullFacing(quad), facing -> new ArrayList<>()).add(quad);
        }

        return new PileOfQuads(quadsByFacing, null, particle);
    }

    /**
     * Splits the vertex colors vanilla baked into the color multiplier, the directional shading and the quad's own
     * color.
     */
    private static void separateColors(ModelQuad quad, int colorMultiplier) {
        float multiplierRed = (colorMultiplier >> 16) & 0xFF;
        float multiplierGreen = (colorMultiplier >> 8) & 0xFF;
        float multiplierBlue = colorMultiplier & 0xFF;
        float multiplierMax = Math.max(multiplierRed, Math.max(multiplierGreen, multiplierBlue));

        boolean tinted = (colorMultiplier & 0xFFFFFF) != 0xFFFFFF && multiplierMax > 0
                && isTinted(quad, multiplierRed, multiplierGreen, multiplierBlue, multiplierMax);

        // the brightness factor of each vertex, IE vanilla's shading
        float[] shadeByVertex = new float[4];
        for (int vertex = 0; vertex < 4; vertex++) {
            // the tessellator stores ABGR
            int color = quad.getColor(vertex);
            float red = color & 0xFF;
            float green = (color >> 8) & 0xFF;
            float blue = (color >> 16) & 0xFF;

            if (tinted) {
                shadeByVertex[vertex] = Math.max(red, Math.max(green, blue)) / multiplierMax;
                continue;
            }

            // untinted shading is the same on all channels, anything else is the quad's own color
            if (Math.abs(red - green) > COLOR_TOLERANCE || Math.abs(green - blue) > COLOR_TOLERANCE) {
                quad.setColorIndex(-1);
                quad.setDirectionalShading(false);
                return;
            }
            shadeByVertex[vertex] = green / 255.0f;
        }
        quad.setColorIndex(tinted ? 0 : -1);

        float tolerance = COLOR_TOLERANCE / (tinted ? multiplierMax : 255.0f);
        float faceShade = getVanillaShade(quad.getNormalFace());
        if (allNear(shadeByVertex, faceShade, tolerance)) {
            setColor(quad, WHITE);
            quad.setDirectionalShading(true);
        } else if (allNear(shadeByVertex, 1.0f, tolerance)) {
            // unshaded, IE crossed squares
            setColor(quad, WHITE);
            quad.setDirectionalShading(false);
        } else {
            // shaded some other way, kept as it is
            for (int vertex = 0; vertex < 4; vertex++) {
                int gray = Math.min(255, Math.round(shadeByVertex[vertex] * 255.0f));
                quad.setColor(vertex, 0xFF000000 | (gray << 16) | (gray << 8) | gray);
            }
            quad.setDirectionalShading(false);
        }
    }

    /** @return true if every vertex is the multiplier times one factor for all channels */
    private static boolean isTinted(ModelQuad quad, float multiplierRed, float multiplierGreen, float multiplierBlue,
            float multiplierMax) {
        for (int vertex = 0; vertex < 4; vertex++) {
            // the tessellator stores ABGR
            int color = quad.getColor(vertex);
            float red = color & 0xFF;
            float green = (color >> 8) & 0xFF;
            float blue = (color >> 16) & 0xFF;

            float shade = Math.max(red, Math.max(green, blue)) / multiplierMax;
            if (Math.abs(red - (shade * multiplierRed)) > COLOR_TOLERANCE
                    || Math.abs(green - (shade * multiplierGreen)) > COLOR_TOLERANCE
                    || Math.abs(blue - (shade * multiplierBlue)) > COLOR_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /** the brightness {@link RenderBlocks} gives faces pointing in the direction */
    private static float getVanillaShade(ModelQuadFacing facing) {
        return switch (facing) {
            case NEG_Y -> 0.5f;
            case NEG_Z, POS_Z -> 0.8f;
            case NEG_X, POS_X -> 0.6f;
            default -> 1.0f;
        };
    }

    private static void setColor(ModelQuad quad, int color) {
        for (int vertex = 0; vertex < 4; vertex++) {
            quad.setColor(vertex, color);
        }
    }

    /** @return the block face the quad lies on, {@link ModelQuadFacing#UNASSIGNED} for none */
    private static ModelQuadFacing getCullFacing(ModelQuadView quad) {
        if (allVerticesNear(quad, 1, 0)) return NEG_Y;
        if (allVerticesNear(quad, 1, 1)) return POS_Y;
        if (allVerticesNear(quad, 2, 0)) return NEG_Z;
        if (allVerticesNear(quad, 2, 1)) return POS_Z;
        if (allVerticesNear(quad, 0, 0)) return NEG_X;
        if (allVerticesNear(quad, 0, 1)) return POS_X;
        return UNASSIGNED;
    }

    /** @param axis 0 for x, 1 for y, 2 for z */
    private static boolean allVerticesNear(ModelQuadView quad, int axis, float target) {
        for (int vertex = 0; vertex < 4; vertex++) {
            float value = switch (axis) {
                case 0 -> quad.getX(vertex);
                case 1 -> quad.getY(vertex);
                default -> quad.getZ(vertex);
            };
            if (Math.abs(value - target) > SIDE_EPSILON) return false;
        }
        return true;
    }

    private static boolean allNear(float[] values, float target, float tolerance) {
        for (float value : values) {
            if (Math.abs(value - target) > tolerance) return false;
        }
        return true;
    }

    /** @return null if no sprite on the block atlas contains the position */
    @Nullable
    private static TextureAtlasSprite findBlockAtlasSprite(float u, float v) {
        ITextureObject blockAtlas = Minecraft.getMinecraft().getTextureManager()
                .getTexture(TextureMap.locationBlocksTexture);
        if (!(blockAtlas instanceof TextureMap textureMap)) return null;

        for (Object value : textureMap.mapUploadedSprites.values()) {
            TextureAtlasSprite sprite = (TextureAtlasSprite) value;
            if (u >= sprite.getMinU() && u <= sprite.getMaxU() && v >= sprite.getMinV() && v <= sprite.getMaxV()) {
                return sprite;
            }
        }
        return null;
    }
}
