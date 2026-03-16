package com.gtnewhorizon.gtnhlib.itemrendering;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

/// Something that can be rendered within an item renderer.
public interface IItemTexture {

    void render(ItemRenderType type, ItemStack stack);
}
