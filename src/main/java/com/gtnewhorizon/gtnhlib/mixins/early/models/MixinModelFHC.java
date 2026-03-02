package com.gtnewhorizon.gtnhlib.mixins.early.models;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.ENTITY;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.INVENTORY;
import static net.minecraftforge.client.IItemRenderer.ItemRendererHelper.ENTITY_BOBBING;
import static net.minecraftforge.client.IItemRenderer.ItemRendererHelper.ENTITY_ROTATION;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(value = ForgeHooksClient.class, remap = false)
public class MixinModelFHC {

    @WrapMethod(method = "renderInventoryItem")
    private static boolean nhlib$renderModeledInventoryItem(RenderBlocks renderBlocks, TextureManager engine,
            ItemStack item, boolean inColor, float zLevel, float x, float y, Operation<Boolean> original) {
        IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(item, INVENTORY);
        if (!(customRenderer instanceof ModelISBRH modelISBRH)) {
            return original.call(renderBlocks, engine, item, inColor, zLevel, x, y);
        }
        engine.bindTexture(
                item.getItemSpriteNumber() == 0 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, -3.0F + zLevel);
        GL11.glScalef(16f, 16f, 16f);
        GL11.glScalef(1.0F, 1.0F, -1F);
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glRotatef(-90f, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-180f, 1.0f, 0.0f, 0.0f);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        if (inColor) {
            int color = item.getItem().getColorFromItemStack(item, 0);
            float r = (float) (color >> 16 & 0xff) / 255F;
            float g = (float) (color >> 8 & 0xff) / 255F;
            float b = (float) (color & 0xff) / 255F;
            GL11.glColor4f(r, g, b, 1.0F);
        }

        renderBlocks.useInventoryTint = inColor;
        modelISBRH.renderItem(INVENTORY, item, renderBlocks);
        renderBlocks.useInventoryTint = true;
        GL11.glPopMatrix();

        return true;
    }

    @WrapMethod(method = "renderEntityItem")
    private static boolean nhlib$renderModeledEntityItem(EntityItem entity, ItemStack item, float bobing,
            float rotation, Random random, TextureManager engine, RenderBlocks renderBlocks, int count,
            Operation<Boolean> original) {
        IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(item, ENTITY);
        if (!(customRenderer instanceof ModelISBRH modelISBRH)) {
            return original.call(entity, item, bobing, rotation, random, engine, renderBlocks, count);
        }

        if (modelISBRH.shouldUseRenderHelper(ENTITY, item, ENTITY_ROTATION)) {
            GL11.glRotatef(rotation, 0.0F, 1.0F, 0.0F);
        }
        if (!modelISBRH.shouldUseRenderHelper(ENTITY, item, ENTITY_BOBBING)) {
            GL11.glTranslatef(0.0F, -bobing, 0.0F);
        }

        engine.bindTexture(
                item.getItemSpriteNumber() == 0 ? TextureMap.locationBlocksTexture : TextureMap.locationItemsTexture);
        Block block = Block.getBlockFromItem(item.getItem());
        boolean blend = block != null && block.getRenderBlockPass() > 0;

        if (RenderItem.renderInFrame) {
            GL11.glScalef(1.25F, 1.25F, 1.25F);
            GL11.glTranslatef(0.0F, 0.05F, 0.0F);
            GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
        }

        if (blend) {
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(770, 771, 1, 0);
        }

        for (int j = 0; j < count; j++) {
            GL11.glPushMatrix();
            if (j > 0) {
                GL11.glTranslatef(
                        ((random.nextFloat() * 2.0F - 1.0F) * 0.2F),
                        ((random.nextFloat() * 2.0F - 1.0F) * 0.2F),
                        ((random.nextFloat() * 2.0F - 1.0F) * 0.2F));
            }
            GL11.glTranslatef(-0.5f, -0.5f, -0.5f);
            modelISBRH.renderItem(ENTITY, item, renderBlocks, entity);
            GL11.glPopMatrix();
        }

        if (blend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
        return true;
    }

    @WrapMethod(method = "renderEquippedItem")
    private static void nhlib$renderModeledEquippedItem(IItemRenderer.ItemRenderType type, IItemRenderer customRenderer,
            RenderBlocks renderBlocks, EntityLivingBase entity, ItemStack item, Operation<Void> original) {
        if (customRenderer instanceof ModelISBRH modelISBRH) {
            if (type == EQUIPPED) {
                GL11.glPushMatrix();

                GL11.glTranslatef(0.5F, 0.5F, 0.5F);
                GL11.glRotatef(135f, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(-75f, 0.0f, 0.0f, 1.0f);
                GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

                GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                GL11.glTranslatef(0.25F, -0.25F, -0.25F);
                GL11.glScaled(1f / 0.375F, 1f / 0.375F, 1f / 0.375F);
                modelISBRH.renderItem(type, item, renderBlocks, entity);

                GL11.glPopMatrix();
                return;
            }

            if (type == EQUIPPED_FIRST_PERSON) {
                GL11.glPushMatrix();

                GL11.glTranslatef(0.5f, 0.5f, 0.5f);
                GL11.glRotatef(-45f, 0.0f, 1.0f, 0.0f);
                GL11.glTranslatef(-0.5f, -0.5f, -0.5f);

                GL11.glTranslatef(-1.45f, -1.25f, 1.75f);
                GL11.glScaled(1f / 0.4f, 1f / 0.4f, 1f / 0.4f);
                GL11.glRotatef(90f, 0.0f, 1.0f, 0.0f);
                modelISBRH.renderItem(type, item, renderBlocks, entity);

                GL11.glPopMatrix();
                return;
            }
        }

        original.call(type, customRenderer, renderBlocks, entity, item);
    }

}
