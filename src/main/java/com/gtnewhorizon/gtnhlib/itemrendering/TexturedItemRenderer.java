package com.gtnewhorizon.gtnhlib.itemrendering;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

/// An [IItemRenderer] that renders an [Item] that implements [ItemWithTextures].
public class TexturedItemRenderer implements IItemRenderer {

    public static final TexturedItemRenderer INSTANCE = new TexturedItemRenderer();

    /// Renders the given item with this system. The item must implement [ItemWithTextures].
    public static <I extends Item & ItemWithTextures> void register(I item) {
        MinecraftForgeClient.registerItemRenderer(item, INSTANCE);
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return type != ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type == ItemRenderType.ENTITY && helper == ItemRendererHelper.ENTITY_BOBBING
                || helper == ItemRendererHelper.ENTITY_ROTATION && Minecraft.getMinecraft().gameSettings.fancyGraphics;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        if (!(stack.getItem() instanceof ItemWithTextures texturedItem)) return;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        ItemRenderUtils.applyStandardItemTransform(type);

        IItemTexture[] textures = texturedItem.getTextures(stack);

        if (textures != null) {
            for (IItemTexture texture : textures) {
                texture.render(type, stack);
            }
        }

        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
