package com.gtnewhorizon.gtnhlib.client.title;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

/**
 * Client-side title/subtitle API. Any mod can call {@link #setTitle}/{@link #setSubtitle} to display overlay text. If a
 * renderer has registered via {@link #registerRenderer()}, it handles full rendering with scale, fade, and subtitle.
 * Otherwise {@link #setTitle} falls back to the vanilla action bar overlay.
 */
@SideOnly(Side.CLIENT)
public class TitleAPI {

    @Getter
    private static volatile IChatComponent title;
    @Getter
    private static volatile IChatComponent subtitle;
    @Getter
    private static volatile int titleTime;
    @Getter
    private static volatile int fadeInTime = 10;
    @Getter
    private static volatile int stayTime = 70;
    @Getter
    private static volatile int fadeOutTime = 20;
    @Getter
    private static volatile boolean rendererRegistered;

    /** Register a full title renderer. Once called, {@link #setTitle} no longer falls back to the action bar. */
    public static void registerRenderer() {
        rendererRegistered = true;
    }

    /** Display a title for {@code fadeIn + stay + fadeOut} ticks. */
    public static void setTitle(IChatComponent component) {
        title = component;
        titleTime = fadeInTime + stayTime + fadeOutTime;
        if (!rendererRegistered) {
            Minecraft.getMinecraft().ingameGUI
                    .func_110326_a(component != null ? component.getFormattedText() : "", false);
        }
    }

    /** Set the subtitle. Only visible while a title is active and a renderer is registered. */
    public static void setSubtitle(IChatComponent component) {
        subtitle = component;
    }

    /** Set fade timing. Negative values leave that field unchanged. If a title is displaying, the timer restarts. */
    public static void setTimes(int fadeIn, int stay, int fadeOut) {
        if (fadeIn >= 0) fadeInTime = fadeIn;
        if (stay >= 0) stayTime = stay;
        if (fadeOut >= 0) fadeOutTime = fadeOut;
        if (titleTime > 0) {
            titleTime = fadeInTime + stayTime + fadeOutTime;
        }
    }

    /** Clear title and subtitle and stop the timer. */
    public static void clear() {
        title = null;
        subtitle = null;
        titleTime = 0;
    }

    /** Reset fade times to defaults (10/70/20). Does not clear the current title. */
    public static void reset() {
        fadeInTime = 10;
        stayTime = 70;
        fadeOutTime = 20;
    }

    /** Decrement the timer once per client tick; clears title and subtitle when it reaches zero. */
    public static void tick() {
        if (titleTime > 0) {
            titleTime--;
            if (titleTime <= 0) {
                title = null;
                subtitle = null;
            }
        }
    }
}
