package com.gtnewhorizon.gtnhlib.itemrendering;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import com.gtnewhorizon.gtnhlib.color.ImmutableColor;

/// An [IItemTexture] that renders an item texture.
public class ItemTexture implements IItemTexture {

    public final Function<ItemStack, IIcon> icon;
    public final Function<ItemStack, ImmutableColor> color;

    public ItemTexture(IIcon icon, ImmutableColor color) {
        this.icon = ignored -> icon;
        this.color = ignored -> color;
    }

    public ItemTexture(Supplier<IIcon> icon, Supplier<ImmutableColor> color) {
        this.icon = ignored -> icon.get();
        this.color = ignored -> color.get();
    }

    public ItemTexture(Function<ItemStack, IIcon> icon, Function<ItemStack, ImmutableColor> color) {
        this.icon = icon;
        this.color = color;
    }

    @Override
    public void render(IItemRenderer.ItemRenderType type, ItemStack stack) {
        ImmutableColor color = this.color.apply(stack);
        IIcon icon = this.icon.apply(stack);

        if (color == null || icon == null) return;

        color.makeActive();
        ItemRenderUtils.renderItem(type, icon);
    }
}
