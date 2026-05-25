package com.gtnewhorizon.gtnhlib.client.title;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;

public class TitleRenderer {

    static final RenderItem itemRender = new RenderItem();

    private static IChatComponent lastTitle = null;

    public static void render(ScaledResolution res, float partialTicks) {

        IChatComponent title = TitleAPI.getTitle();
        int titleTime = TitleAPI.getTitleTime();

        Minecraft mc = Minecraft.getMinecraft();
        mc.mcProfiler.startSection("titleAndSubtitle");

        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        float f = (float) titleTime - partialTicks;
        int fadeIn = TitleAPI.getFadeInTime();
        int stay = TitleAPI.getStayTime();
        int fadeOut = TitleAPI.getFadeOutTime();
        int total = fadeIn + stay + fadeOut;
        int alpha = 255;

        if (titleTime > fadeOut + stay) {
            float totalTime = (float) total;
            alpha = (int) ((totalTime - f) * 255.0F / (float) fadeIn);
        } else if (titleTime <= fadeOut) {
            alpha = (int) (f * 255.0F / (float) fadeOut);
        }

        alpha = MathHelper.clamp_int(alpha, 0, 255);
        if (alpha <= 8) {
            mc.mcProfiler.endSection();
            return;
        }

        float fadeInProgress = -1.0F;
        if (fadeIn > 0 && titleTime > fadeOut + stay) {
            float elapsed = (float) total - f;
            fadeInProgress = MathHelper.clamp_float(elapsed / (float) fadeIn, 0.0F, 1.0F);
        }

        int color = 0xFFFFFF | (alpha << 24);
        float apiTitleScale = TitleAPI.getTitleScale();
        float tScale = apiTitleScale > 0 ? apiTitleScale : GTNHLibConfig.titleScale;
        float apiSubtitleScale = TitleAPI.getSubtitleScale();
        float sScale = apiSubtitleScale > 0 ? apiSubtitleScale : GTNHLibConfig.subtitleScale;

        if (title != lastTitle) {
            lastTitle = title;

            int pe = TitleAPI.getParticleEffect();
            if (pe != TitleParticleSystem.PARTICLE_NONE) {
                TitleParticleSystem.spawn(pe, width / 2, height / 2, TitleAPI.getIcon());
            }
        }

        TitleParticleSystem.render(partialTicks);

        GL11.glPushMatrix();
        GL11.glTranslatef((float) (width / 2), (float) (height / 2), 0.0F);
        GL11.glEnable(GL11.GL_BLEND);

        ItemStack icon = TitleAPI.getIcon();
        if (icon != null && GTNHLibConfig.showTitleIcon) {
            renderIcon(mc, icon, tScale, alpha, fadeInProgress);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPushMatrix();
        GL11.glScalef(tScale, tScale, tScale);
        String titleText = title.getFormattedText();
        final FontRenderer fontrenderer = mc.fontRenderer;
        int titleWidth = fontrenderer.getStringWidth(titleText);
        fontrenderer.drawStringWithShadow(titleText, -titleWidth / 2, -10, color);
        GL11.glPopMatrix();

        IChatComponent subtitle = TitleAPI.getSubtitle();
        if (subtitle != null) {
            GL11.glPushMatrix();
            GL11.glScalef(sScale, sScale, sScale);
            String subText = subtitle.getFormattedText();
            int subWidth = fontrenderer.getStringWidth(subText);
            fontrenderer.drawStringWithShadow(subText, -subWidth / 2, 5, color);
            GL11.glPopMatrix();
        }

        GL11.glPopMatrix();

        mc.mcProfiler.endSection();
    }

    private static int resolveAnimation(int apiValue) {
        if (apiValue >= 0) return apiValue;
        return switch (GTNHLibConfig.titleIconAnimation) {
            case "fly_in" -> TitleAPI.ICON_ANIM_FLY_IN;
            case "spin" -> TitleAPI.ICON_ANIM_SPIN;
            default -> TitleAPI.ICON_ANIM_NONE;
        };
    }

    private static void renderIcon(Minecraft mc, ItemStack icon, float titleScale, int alpha, float fadeInProgress) {
        GL11.glPushMatrix();

        float iconSize = 16.0F;
        float apiScale = TitleAPI.getIconScale();
        float iconRenderScale = apiScale > 0 ? apiScale : GTNHLibConfig.titleIconScale;
        float scaledIconSize = iconSize * iconRenderScale;
        float titleTopY = -10.0F * titleScale;
        int apiOffset = TitleAPI.getIconOffsetY();
        int offsetY = apiOffset != Integer.MIN_VALUE ? apiOffset : GTNHLibConfig.titleIconOffsetY;
        float iconY = titleTopY - scaledIconSize - 4.0F + offsetY;
        float iconCenterX = 0.0F;
        float iconCenterY = iconY + scaledIconSize / 2.0F;

        int animStyle = resolveAnimation(TitleAPI.getIconAnimation());
        boolean animating = animStyle != TitleAPI.ICON_ANIM_NONE && fadeInProgress >= 0.0F && fadeInProgress < 1.0F;
        float animScale = 1.0F;

        if (animating) {
            float eased = 1.0F - (1.0F - fadeInProgress) * (1.0F - fadeInProgress);

            if (animStyle == TitleAPI.ICON_ANIM_FLY_IN) {
                float flyDistance = 40.0F * iconRenderScale;
                float flyOffset = -flyDistance * (1.0F - eased);
                GL11.glTranslatef(iconCenterX, iconCenterY + flyOffset, 0.0F);
            } else if (animStyle == TitleAPI.ICON_ANIM_SPIN) {
                GL11.glTranslatef(iconCenterX, iconCenterY, 0.0F);
                animScale = 1.0F + 2.0F * (1.0F - eased);
                GL11.glRotatef(720.0F * eased, 0.0F, 0.0F, 1.0F);
            }
        } else {
            GL11.glTranslatef(iconCenterX, iconCenterY, 0.0F);
        }

        float finalScale = iconRenderScale * animScale;
        GL11.glTranslatef(-iconSize / 2.0F * finalScale, -iconSize / 2.0F * finalScale, 0.0F);
        GL11.glScalef(finalScale, finalScale, 1.0F);

        float a = alpha / 255.0F;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, a);

        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        float preZ = itemRender.zLevel;
        itemRender.zLevel = -50F;

        FontRenderer font = icon.getItem().getFontRenderer(icon);
        if (font == null) font = mc.fontRenderer;

        try {
            itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), icon, 0, 0);
        } catch (Exception ignored) {}

        itemRender.zLevel = preZ;

        RenderHelper.disableStandardItemLighting();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glPopMatrix();
    }
}
