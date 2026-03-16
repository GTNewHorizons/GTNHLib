package com.gtnewhorizon.gtnhlib.itemrendering;

import net.minecraft.item.ItemStack;

/// An [net.minecraft.item.Item] that can be rendered via the [TexturedItemRenderer].
public interface ItemWithTextures {

    /// Gets the item textures for a given stack. These are rendered in order. The lower indices are rendered prior to
    /// the higher indices.
    IItemTexture[] getTextures(ItemStack stack);
}
