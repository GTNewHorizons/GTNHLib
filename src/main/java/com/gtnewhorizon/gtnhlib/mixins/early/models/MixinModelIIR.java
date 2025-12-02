package com.gtnewhorizon.gtnhlib.mixins.early.models;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY;

@Mixin(value = ForgeHooksClient.class, remap = false)
public class MixinModelIIR {

    @WrapMethod(method = "renderInventoryItem")
    private static boolean nhlib$renderInventoryItem(RenderBlocks renderBlocks, TextureManager engine, ItemStack item, boolean inColor, float zLevel, float x, float y, Operation<Boolean> original) {
        IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(item, INVENTORY);
        if (!(customRenderer instanceof ModelISBRH modelISBRH))
        {
            return original.call(renderBlocks,engine,item,inColor,zLevel,x,y);
        }
        engine.bindTexture(item.getItemSpriteNumber() == 0 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y ,  -3.0F + zLevel);
        GL11.glScalef(16f, 16f, 16f);
        GL11.glScalef(1.0F, 1.0F, -1F);
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glRotatef(-90f, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-180f, 1.0f, 0.0f, 0.0f);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        if(inColor)
        {
            int color = item.getItem().getColorFromItemStack(item, 0);
            float r = (float)(color >> 16 & 0xff) / 255F;
            float g = (float)(color >> 8 & 0xff) / 255F;
            float b = (float)(color & 0xff) / 255F;
            GL11.glColor4f(r, g, b, 1.0F);
        }

        renderBlocks.useInventoryTint = inColor;
        modelISBRH.renderItem(INVENTORY, item, renderBlocks);
        renderBlocks.useInventoryTint = true;
        GL11.glPopMatrix();

        return true;
    }


}
