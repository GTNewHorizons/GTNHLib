package com.gtnewhorizon.gtnhlib.itemrendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

public class ItemRenderUtils {

    public static void applyStandardItemTransform(IItemRenderer.ItemRenderType type) {
        if (type == IItemRenderer.ItemRenderType.ENTITY) {
            if (RenderItem.renderInFrame) {
                // Magic numbers calculated from vanilla code
                GL11.glScalef(1.025641F, 1.025641F, 1.025641F);
                GL11.glTranslatef(0.0F, -0.05F, 0.0F);
            }

            if (Minecraft.getMinecraft().gameSettings.fancyGraphics) {
                if (RenderItem.renderInFrame) {
                    GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
                }
                // Magic numbers calculated from vanilla code
                GL11.glTranslatef(-0.5F, -0.25F, 0.0421875F);
            }
        }
    }

    public static void renderItemIcon(IIcon icon, double size, double z, float nx, float ny, float nz) {
        renderItemIcon(icon, 0.0D, 0.0D, size, size, z, nx, ny, nz);
    }

    public static void renderItemIcon(IIcon icon, double xStart, double yStart, double xEnd, double yEnd, double z,
            float nx, float ny, float nz) {
        if (icon == null) {
            return;
        }
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setNormal(nx, ny, nz);
        if (nz > 0.0F) {
            tessellator.addVertexWithUV(xStart, yStart, z, icon.getMinU(), icon.getMinV());
            tessellator.addVertexWithUV(xEnd, yStart, z, icon.getMaxU(), icon.getMinV());
            tessellator.addVertexWithUV(xEnd, yEnd, z, icon.getMaxU(), icon.getMaxV());
            tessellator.addVertexWithUV(xStart, yEnd, z, icon.getMinU(), icon.getMaxV());
        } else {
            tessellator.addVertexWithUV(xStart, yEnd, z, icon.getMinU(), icon.getMaxV());
            tessellator.addVertexWithUV(xEnd, yEnd, z, icon.getMaxU(), icon.getMaxV());
            tessellator.addVertexWithUV(xEnd, yStart, z, icon.getMaxU(), icon.getMinV());
            tessellator.addVertexWithUV(xStart, yStart, z, icon.getMinU(), icon.getMinV());
        }
        tessellator.draw();
    }

    public static void renderItem(IItemRenderer.ItemRenderType type, IIcon icon) {
        Tessellator tessellator = Tessellator.instance;
        float maxU = icon.getMaxU();
        float minV = icon.getMinV();
        float minU = icon.getMinU();
        float maxV = icon.getMaxV();

        switch (type) {
            case ENTITY -> {
                if (Minecraft.getMinecraft().gameSettings.fancyGraphics) {
                    ItemRenderer.renderItemIn2D(
                            tessellator,
                            maxU,
                            minV,
                            minU,
                            maxV,
                            icon.getIconWidth(),
                            icon.getIconHeight(),
                            0.0625F);
                } else {
                    GL11.glPushMatrix();

                    if (!RenderItem.renderInFrame) {
                        GL11.glRotatef(180.0F - RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
                    }

                    tessellator.startDrawingQuads();
                    tessellator.setNormal(0.0F, 1.0F, 0.0F);
                    tessellator.addVertexWithUV(0.0F - 0.5F, 0.0F - 0.25F, 0.0D, minU, maxV);
                    tessellator.addVertexWithUV(1.0F - 0.5F, 0.0F - 0.25F, 0.0D, maxU, maxV);
                    tessellator.addVertexWithUV(1.0F - 0.5F, 1.0F - 0.25F, 0.0D, maxU, minV);
                    tessellator.addVertexWithUV(0.0F - 0.5F, 1.0F - 0.25F, 0.0D, minU, minV);
                    tessellator.draw();

                    GL11.glPopMatrix();
                }
            }
            case EQUIPPED, EQUIPPED_FIRST_PERSON -> {
                ItemRenderer.renderItemIn2D(
                        tessellator,
                        maxU,
                        minV,
                        minU,
                        maxV,
                        icon.getIconWidth(),
                        icon.getIconHeight(),
                        0.0625F);
            }
            case INVENTORY -> {
                renderItemIcon(icon, 16.0D, 0.001, 0.0F, 0.0F, -1.0F);
            }
            default -> {}
        }
    }
}
