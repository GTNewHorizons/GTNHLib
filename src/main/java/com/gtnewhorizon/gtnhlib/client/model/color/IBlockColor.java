package com.gtnewhorizon.gtnhlib.client.model.color;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

/// Interface for providing color multipliers for blocks and item blocks. Implement this interface on your block to
/// provide custom coloring logic for a block or its item form.
public interface IBlockColor {

    /// Returns the color multiplier for a block in the world at the given position. -1 is 0xFFFFFFFF, or white - i.e.
    /// no color change.
    ///
    /// @param world The IBlockAccess world instance.
    /// @param x Block X coordinate.
    /// @param y Block Y coordinate.
    /// @param z Block Z coordinate.
    /// @param tintIndex The tint index, usually 0 for the main texture.
    /// @return Color multiplier as 0xRRGGBB.
    default int colorMultiplier(@Nullable IBlockAccess world, int x, int y, int z, int tintIndex) {
        return -1;
    }

    /// Returns the color multiplier for an item stack. -1 is 0xFFFFFFFF, or white - i.e. no color change.
    ///
    /// @param stack The ItemStack instance.
    /// @param tintIndex The tint index, usually 0 for the main texture.
    /// @return Color multiplier as 0xRRGGBB.
    default int colorMultiplier(@Nullable ItemStack stack, int tintIndex) {
        return -1;
    }
}
