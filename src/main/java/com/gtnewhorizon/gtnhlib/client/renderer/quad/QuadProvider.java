package com.gtnewhorizon.gtnhlib.client.renderer.quad;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

public interface QuadProvider {

    int R_MASK = 0xFF << 16;
    int G_MASK = 0xFF << 8;
    int B_MASK = 0xFF;

    /**
     * Called once per block and returns the color packed as ABGR. If you want to vary colors within a block, just
     * ignore the color passed into {@link #getQuads} and use your own function, don't override this. But if you only
     * want to change color once per block, like vanilla leaves, override this. The default vanilla adapter is
     * {@link #getDefaultColor}
     */
    default int getColor(IBlockAccess world, int x, int y, int z, Block block, int meta, Random random) {

        return -1;
    }

    /**
     * Defers to the vanilla color functions, just repacks the color to ABGR as models require.
     */
    static int getDefaultColor(IBlockAccess world, int x, int y, int z, Block block) {

        final int cin = block.colorMultiplier(world, x, y, z);
        return (0xFF << 24) | ((cin & B_MASK) << 16) | (cin & G_MASK) | ((cin & R_MASK) >>> 16);
    }

    /**
     * If you need to allocate new quads, set this to true. Then, the quads returned by {@link #getQuads} are recycled,
     * and you should not keep a reference to them. Example: stone can return a static list every time, but a modded
     * block which adds or removes quads based on location would likely need dynamic quads.
     */
    default boolean isDynamic() {
        return false;
    }

    /**
     * Provide quads to render. If you need new quads, they should be obtained with the passed supplier,
     * {@link #isDynamic} should be overridden to return true, and you should not keep references to the quads. The
     * quads produced are not offset according to the passed xyz - that is up to the renderer.
     *
     * @param world    The world the model is in. Null if not applicable, like in inventory rendering.
     * @param x        The x-position of the model.
     * @param y        The y-position of the model.
     * @param z        The z-position of the model.
     * @param block    The block to be rendered.
     * @param meta     The meta value of the rendered object.
     * @param dir      The direction to get quads from - the returned quads should have the same facing.
     * @param random   A RNG to use.
     * @param color    The ABGR packed color to use.
     * @param quadPool If {@link #isDynamic()}, the pool to obtain quads from and where the quads should be released
     *                 after use. Ignored otherwise.
     * @return A list of quads from the model. These are *not* adjusted for location - that is up to the renderer.
     */
    List<QuadView> getQuads(@Nullable IBlockAccess world, int x, int y, int z, Block block, int meta,
            ForgeDirection dir, Random random, int color, @Nullable Supplier<QuadView> quadPool);
}
