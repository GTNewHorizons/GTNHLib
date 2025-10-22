package com.gtnewhorizon.gtnhlib.client.model.baked;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.color.ImmutableColor;
import com.gtnewhorizon.gtnhlib.color.RGBColor;

public interface BakedModel {

    /// Called once per render call/pass and returns the color. If you want to vary colors within a block, just ignore the color
    /// passed into [#getQuads] and use your own function, don't override this. But if you only want to change color
    /// once per block, like vanilla leaves, override this. The default vanilla adapter is [#getDefaultColor]
    default ImmutableColor getColor(BakedModelQuadContext context) {
        if (context instanceof BakedModelQuadContext.World world) {
            return getDefaultColor(world.getWorld(), world.getX(), world.getY(), world.getZ(), world.getBlockState().getBlock());
        } else if (context instanceof BakedModelQuadContext.Item item) {
            Item item1 = item.getItemStack().getItem();
            assert item1 != null;
            return RGBColor.fromRGB(item1.getColorFromItemStack(item.getItemStack(), 0));
        }

        return RGBColor.WHITE;
    }

    static ImmutableColor getDefaultColor(IBlockAccess world, int x, int y, int z, Block block) {
        return RGBColor.fromRGB(block.colorMultiplier(world, x, y, z));
    }

    /// If you need to allocate new quads, set this to true. Then, the quads returned by [#getQuads] are recycled,
    /// and you should not keep a reference to them. Example: stone can return a static list every time, but a modded
    /// block which adds or removes quads based on location would likely need dynamic quads.
    default boolean isDynamic() {
        return false;
    }

    /// Provide quads to render. If you need new quads, they should be obtained with the passed supplier,
    /// [#isDynamic] should be overridden to return true, and you should not keep references to the quads.
    ///
    /// @param context The context within which to get the quads.
    /// @return A list of quads from the model. These are *not* adjusted for the given xyz - that is up to the renderer.
    List<ModelQuadView> getQuads(BakedModelQuadContext context);
}
