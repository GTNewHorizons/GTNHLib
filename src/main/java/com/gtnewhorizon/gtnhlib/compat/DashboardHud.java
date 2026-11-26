package com.gtnewhorizon.gtnhlib.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IChatComponent;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.client.title.TitleAPI;
import com.gtnewhorizons.dashboard.api.HudBounds;
import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Lets players move, resize and hide the text above the hotbar and the titles in Dashboard's HUD editor. The only class
 * using Dashboard, so only call it while Dashboard is loaded.
 */
@SideOnly(Side.CLIENT)
public final class DashboardHud {

    private static final int HOTBAR_WIDTH = 182;
    private static final int TEXT_HEIGHT = 8;
    /** The title is drawn 10 text pixels above its spot and the subtitle 5 below, each at its own size. */
    private static final int TITLE_TOP = -10;
    private static final int SUBTITLE_TOP = 5;
    private static final String SAMPLE_TITLE = "Title";
    private static final String SAMPLE_SUBTITLE = "Subtitle";
    private static final String SAMPLE_TEXT = "Text above the hotbar";

    /** Other mods drawing at the same spot can look it up by its id and draw with it. */
    private static final HudElement ABOVE_HOTBAR_TEXT = new HudElement(
            "gtnhlib:above_hotbar_text",
            (screenWidth, screenHeight) -> new HudBounds(
                    screenWidth / 2 - HOTBAR_WIDTH / 2,
                    screenHeight - 70,
                    HOTBAR_WIDTH,
                    TEXT_HEIGHT));

    /** The particles, effects, icon, title and subtitle all move together. */
    private static final HudElement TITLE = new HudElement("gtnhlib:title", DashboardHud::getTitleBounds);

    private DashboardHud() {}

    public static void register() {
        ABOVE_HOTBAR_TEXT.setPreview(
                (screenWidth, screenHeight) -> HudRegistry.render(
                        ABOVE_HOTBAR_TEXT,
                        () -> drawAboveHotbarText(ABOVE_HOTBAR_TEXT.getDefaultBounds(screenWidth, screenHeight))));
        TITLE.setPreview(
                (screenWidth, screenHeight) -> HudRegistry.render(TITLE, () -> drawTitle(screenWidth, screenHeight)));
        HudRegistry.register(ABOVE_HOTBAR_TEXT);
        HudRegistry.register(TITLE);
    }

    /** False means the player hid the text, so skip drawing it and do not call {@link #endRender()}. */
    public static boolean beginAboveHotbarText(ScaledResolution resolution) {
        return HudRegistry.beginRender(ABOVE_HOTBAR_TEXT, resolution);
    }

    /** False means the player hid the title, so skip drawing it and do not call {@link #endRender()}. */
    public static boolean beginTitle(ScaledResolution resolution) {
        return HudRegistry.beginRender(TITLE, resolution);
    }

    public static void endRender() {
        HudRegistry.endRender();
    }

    /** As wide as the current title or subtitle, or the sample, and from the title's top to the subtitle's bottom. */
    private static HudBounds getTitleBounds(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        float titleScale = getTitleScale();
        float subtitleScale = getSubtitleScale();
        int width = Math.round(
                Math.max(
                        font.getStringWidth(getTitleText()) * titleScale,
                        font.getStringWidth(getSubtitleText()) * subtitleScale));
        int top = Math.round(TITLE_TOP * titleScale);
        int bottom = Math.round((SUBTITLE_TOP + TEXT_HEIGHT) * subtitleScale);
        return new HudBounds(
                getTitleCenterX(screenWidth) - width / 2,
                getTitleCenterY(screenHeight) + top,
                width,
                bottom - top);
    }

    /** Like TitleRenderer: what the current title asks for, else the config. */
    private static float getTitleScale() {
        return TitleAPI.getTitleScale() > 0 ? TitleAPI.getTitleScale() : GTNHLibConfig.titleScale;
    }

    private static float getSubtitleScale() {
        return TitleAPI.getSubtitleScale() > 0 ? TitleAPI.getSubtitleScale() : GTNHLibConfig.subtitleScale;
    }

    private static int getTitleCenterX(int screenWidth) {
        int offset = TitleAPI.getTitleOffsetX();
        return screenWidth / 2 + (offset != Integer.MIN_VALUE ? offset : GTNHLibConfig.titleOffsetX);
    }

    private static int getTitleCenterY(int screenHeight) {
        int offset = TitleAPI.getTitleOffsetY();
        return screenHeight / 2 + (offset != Integer.MIN_VALUE ? offset : GTNHLibConfig.titleOffsetY);
    }

    private static String getTitleText() {
        IChatComponent title = TitleAPI.getTitle();
        return title != null ? title.getFormattedText() : SAMPLE_TITLE;
    }

    private static String getSubtitleText() {
        IChatComponent subtitle = TitleAPI.getSubtitle();
        return subtitle != null ? subtitle.getFormattedText() : SAMPLE_SUBTITLE;
    }

    private static void drawAboveHotbarText(HudBounds bounds) {
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(SAMPLE_TEXT);
        drawScaledString(SAMPLE_TEXT, bounds.x + (bounds.width - width) / 2f, bounds.y, 1);
    }

    /** The sample title and subtitle, centered on the title's spot at their sizes. */
    private static void drawTitle(int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int centerX = getTitleCenterX(screenWidth);
        int centerY = getTitleCenterY(screenHeight);
        float titleScale = getTitleScale();
        float subtitleScale = getSubtitleScale();
        drawScaledString(
                SAMPLE_TITLE,
                centerX - font.getStringWidth(SAMPLE_TITLE) * titleScale / 2,
                centerY + TITLE_TOP * titleScale,
                titleScale);
        drawScaledString(
                SAMPLE_SUBTITLE,
                centerX - font.getStringWidth(SAMPLE_SUBTITLE) * subtitleScale / 2,
                centerY + SUBTITLE_TOP * subtitleScale,
                subtitleScale);
    }

    private static void drawScaledString(String text, float x, float y, float scale) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1);
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, 0, 0, 0xFFFFFF);
        GL11.glPopMatrix();
    }
}
