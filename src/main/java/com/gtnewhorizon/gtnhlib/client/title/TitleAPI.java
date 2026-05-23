package com.gtnewhorizon.gtnhlib.client.title;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

/**
 * Client-side title/subtitle API. Any mod can call {@link #setTitle}/{@link #setSubtitle} to display overlay text. The
 * renderer is shipped in this lib and runs whenever a title is active.
 */
@SideOnly(Side.CLIENT)
public class TitleAPI {

    public static final int ICON_ANIM_DEFAULT = -1;
    public static final int ICON_ANIM_NONE = 0;
    public static final int ICON_ANIM_FLY_IN = 1;
    public static final int ICON_ANIM_SPIN = 2;

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
    private static volatile ItemStack icon;
    @Getter
    private static volatile float iconScale;
    @Getter
    private static volatile int iconOffsetY = Integer.MIN_VALUE;
    @Getter
    private static volatile float titleScale;
    @Getter
    private static volatile float subtitleScale;
    @Getter
    private static volatile int iconAnimation = ICON_ANIM_DEFAULT;
    @Getter
    private static volatile int particleEffect;
    @Getter
    private static volatile ItemStack confettiIcon;
    @Getter
    private static volatile int particleCount = -1;

    /** Display a title for {@code fadeIn + stay + fadeOut} ticks. */
    public static void setTitle(IChatComponent component) {
        title = component;
        titleTime = fadeInTime + stayTime + fadeOutTime;
    }

    /** Set the subtitle. Only visible while a title is active. */
    public static void setSubtitle(IChatComponent component) {
        subtitle = component;
    }

    /** Set the item icon displayed above the title. Pass null to clear. */
    public static void setIcon(ItemStack stack) {
        icon = stack;
    }

    /** Set icon scale. 0 or negative = use client config default. */
    public static void setIconScale(float scale) {
        iconScale = scale;
    }

    /** Set icon vertical offset in pixels. {@code Integer.MIN_VALUE} = use client config default. */
    public static void setIconOffsetY(int offset) {
        iconOffsetY = offset;
    }

    /** Set title text scale. 0 or negative = use client config default. */
    public static void setTitleScale(float scale) {
        titleScale = scale;
    }

    /** Set subtitle text scale. 0 or negative = use client config default. */
    public static void setSubtitleScale(float scale) {
        subtitleScale = scale;
    }

    /** Set icon animation style. Use {@code ICON_ANIM_*} constants. {@code ICON_ANIM_DEFAULT} = use client config. */
    public static void setIconAnimation(int animation) {
        iconAnimation = animation;
    }

    /** Set particle effect. Use {@code TitleParticleSystem.PARTICLE_*} constants. */
    public static void setParticleEffect(int effect) {
        particleEffect = effect;
    }

    /** Set the item used for item confetti particles. Null = use main title icon. */
    public static void setConfettiIcon(ItemStack stack) {
        confettiIcon = stack;
    }

    /** Set particle count override. Negative = use default count for the effect. */
    public static void setParticleCount(int count) {
        particleCount = count;
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
        icon = null;
        iconScale = 0;
        iconOffsetY = Integer.MIN_VALUE;
        titleScale = 0;
        subtitleScale = 0;
        iconAnimation = ICON_ANIM_DEFAULT;
        particleEffect = 0;
        confettiIcon = null;
        particleCount = -1;
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
                icon = null;
                iconScale = 0;
                iconOffsetY = Integer.MIN_VALUE;
                titleScale = 0;
                subtitleScale = 0;
                iconAnimation = ICON_ANIM_DEFAULT;
                particleEffect = 0;
                confettiIcon = null;
                particleCount = -1;
            }
        }
    }
}
