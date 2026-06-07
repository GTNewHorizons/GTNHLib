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

        // Animation progress: 0 = fully entering, 1 = settled. During fade-out it runs 1 -> 0 so the
        // exit replays the entrance in reverse (spin in clockwise -> spin out counter-clockwise, etc.).
        float animQ;
        if (titleTime > fadeOut + stay) {
            animQ = fadeIn > 0 ? MathHelper.clamp_float(((float) total - f) / (float) fadeIn, 0.0F, 1.0F) : 1.0F;
        } else if (titleTime <= fadeOut) {
            animQ = fadeOut > 0 ? MathHelper.clamp_float(f / (float) fadeOut, 0.0F, 1.0F) : 1.0F;
        } else {
            animQ = 1.0F;
        }

        int color = 0xFFFFFF | (alpha << 24);
        float apiTitleScale = TitleAPI.getTitleScale();
        float tScale = apiTitleScale > 0 ? apiTitleScale : GTNHLibConfig.titleScale;
        float apiSubtitleScale = TitleAPI.getSubtitleScale();
        float sScale = apiSubtitleScale > 0 ? apiSubtitleScale : GTNHLibConfig.subtitleScale;

        int apiOffX = TitleAPI.getTitleOffsetX();
        int apiOffY = TitleAPI.getTitleOffsetY();
        int originX = width / 2 + (apiOffX != Integer.MIN_VALUE ? apiOffX : GTNHLibConfig.titleOffsetX);
        int originY = height / 2 + (apiOffY != Integer.MIN_VALUE ? apiOffY : GTNHLibConfig.titleOffsetY);

        int apiTier = TitleAPI.getEffectTier();
        int effectTier = apiTier > 0 ? apiTier : GTNHLibConfig.titleEffectTier;

        if (title != lastTitle) {
            lastTitle = title;

            int pe = TitleAPI.getParticleEffect();
            if (pe != TitleParticleSystem.PARTICLE_NONE) {
                TitleParticleSystem.spawn(pe, originX, originY, TitleAPI.getIcon());
            }
            TitleEffectSystem.spawn(effectTier, originX, originY, width, height);
        }

        TitleParticleSystem.render(partialTicks);
        float t01 = total > 0 ? MathHelper.clamp_float((total - f) / (float) total, 0.0F, 1.0F) : 1.0F;
        float fInEnd = total > 0 ? (float) fadeIn / (float) total : 0.0F;
        float fOutStart = total > 0 ? (float) (fadeIn + stay) / (float) total : 1.0F;
        TitleEffectSystem.render(t01, fInEnd, fOutStart, alpha / 255.0F);

        // the effect can shake + scale-punch the whole title block on a "jump"
        float shakeX = TitleEffectSystem.getShakeX();
        float shakeY = TitleEffectSystem.getShakeY();
        float pulse = TitleEffectSystem.getScalePulse();
        tScale *= pulse;
        sScale *= pulse;

        GL11.glPushMatrix();
        GL11.glTranslatef((float) originX + shakeX, (float) originY + shakeY, 0.0F);
        GL11.glEnable(GL11.GL_BLEND);

        ItemStack icon = TitleAPI.getIcon();
        if (icon != null && GTNHLibConfig.showTitleIcon) {
            renderIcon(mc, icon, tScale, alpha, animQ);
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

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();

        mc.mcProfiler.endSection();
    }

    private static int resolveAnimation(int apiValue) {
        if (apiValue >= 0) return apiValue;
        return switch (GTNHLibConfig.titleIconAnimation) {
            case "fly_in" -> TitleAPI.ICON_ANIM_FLY_IN;
            case "spin" -> TitleAPI.ICON_ANIM_SPIN;
            case "spin_reverse" -> TitleAPI.ICON_ANIM_SPIN_REVERSE;
            case "rise" -> TitleAPI.ICON_ANIM_RISE;
            case "slide" -> TitleAPI.ICON_ANIM_SLIDE;
            case "zoom" -> TitleAPI.ICON_ANIM_ZOOM;
            case "pop" -> TitleAPI.ICON_ANIM_POP;
            case "bounce" -> TitleAPI.ICON_ANIM_BOUNCE;
            case "wobble" -> TitleAPI.ICON_ANIM_WOBBLE;
            case "swing" -> TitleAPI.ICON_ANIM_SWING;
            case "slam" -> TitleAPI.ICON_ANIM_SLAM;
            case "tada" -> TitleAPI.ICON_ANIM_TADA;
            default -> TitleAPI.ICON_ANIM_NONE;
        };
    }

    private static void renderIcon(Minecraft mc, ItemStack icon, float titleScale, int alpha, float animQ) {
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

        // q = 1 is the settled icon (identity); each style deforms toward q = 0. Entrance runs q 0->1,
        // exit runs q 1->0, so the exit is the entrance in reverse.
        int animStyle = resolveAnimation(TitleAPI.getIconAnimation());
        float dist = 40.0F * iconRenderScale;
        float e = easeOut(animQ);
        float dx = 0.0F, dy = 0.0F, rot = 0.0F, scaleMul = 1.0F;
        switch (animStyle) {
            case TitleAPI.ICON_ANIM_FLY_IN -> dy = -dist * (1.0F - e);
            case TitleAPI.ICON_ANIM_RISE -> dy = dist * (1.0F - e);
            case TitleAPI.ICON_ANIM_SLIDE -> dx = -dist * (1.0F - e);
            case TitleAPI.ICON_ANIM_ZOOM -> scaleMul = Math.max(0.0F, e);
            case TitleAPI.ICON_ANIM_POP -> scaleMul = Math.max(0.0F, easeOutBack(animQ));
            case TitleAPI.ICON_ANIM_SPIN -> {
                rot = 720.0F * e;
                scaleMul = 1.0F + 2.0F * (1.0F - e);
            }
            case TitleAPI.ICON_ANIM_SPIN_REVERSE -> {
                rot = -720.0F * e;
                scaleMul = 1.0F + 2.0F * (1.0F - e);
            }
            case TitleAPI.ICON_ANIM_BOUNCE -> dy = -dist * 1.25F * (1.0F - easeOutBounce(animQ));
            case TitleAPI.ICON_ANIM_WOBBLE -> scaleMul = Math.max(0.0F, easeOutElastic(animQ));
            case TitleAPI.ICON_ANIM_SWING -> rot = -80.0F * (1.0F - easeOutBack(animQ));
            case TitleAPI.ICON_ANIM_SLAM -> scaleMul = 1.0F + 3.0F * (1.0F - animQ) * (1.0F - animQ);
            case TitleAPI.ICON_ANIM_TADA -> {
                scaleMul = Math.max(0.0F, easeOutBack(animQ));
                rot = 12.0F * (float) Math.sin(animQ * Math.PI * 5.0) * (1.0F - animQ);
            }
            default -> {}
        }

        GL11.glTranslatef(iconCenterX + dx, iconCenterY + dy, 0.0F);
        if (rot != 0.0F) GL11.glRotatef(rot, 0.0F, 0.0F, 1.0F);

        float finalScale = iconRenderScale * scaleMul;
        GL11.glTranslatef(-iconSize / 2.0F * finalScale, -iconSize / 2.0F * finalScale, 0.0F);
        GL11.glScalef(finalScale, finalScale, 1.0F);

        float a = alpha / 255.0F;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, a);

        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // renderItemIntoGUI overwrites glColor alpha with 1 when renderWithColor is on; turn it off so
        // the fade alpha survives. It already blends SRC_ALPHA/ONE_MINUS_SRC_ALPHA.
        boolean prevRenderWithColor = itemRender.renderWithColor;
        itemRender.renderWithColor = false;
        // alpha func 0.1 clips the icon before the fade reaches 0
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);

        float preZ = itemRender.zLevel;
        itemRender.zLevel = -50F;

        FontRenderer font = icon.getItem().getFontRenderer(icon);
        if (font == null) font = mc.fontRenderer;

        try {
            itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), icon, 0, 0);
        } catch (Exception ignored) {}

        itemRender.zLevel = preZ;

        itemRender.renderWithColor = prevRenderWithColor;
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);

        RenderHelper.disableStandardItemLighting();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glPopMatrix();
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float u = t - 1.0F;
        return 1.0F + c3 * u * u * u + c1 * u * u;
    }

    private static float easeOutBounce(float t) {
        float n1 = 7.5625F;
        float d1 = 2.75F;
        if (t < 1.0F / d1) {
            return n1 * t * t;
        } else if (t < 2.0F / d1) {
            t -= 1.5F / d1;
            return n1 * t * t + 0.75F;
        } else if (t < 2.5F / d1) {
            t -= 2.25F / d1;
            return n1 * t * t + 0.9375F;
        } else {
            t -= 2.625F / d1;
            return n1 * t * t + 0.984375F;
        }
    }

    private static float easeOutElastic(float t) {
        if (t <= 0.0F) return 0.0F;
        if (t >= 1.0F) return 1.0F;
        float c4 = (float) (2.0 * Math.PI) / 3.0F;
        return (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0);
    }
}
