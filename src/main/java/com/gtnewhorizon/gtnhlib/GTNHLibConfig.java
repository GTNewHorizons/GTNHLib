package com.gtnewhorizon.gtnhlib;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = GTNHLib.MODID)
public class GTNHLibConfig {

    @Config.Comment("Set to true to no longer check if the NEI version is new enough to support RenderTooltipEvents")
    @Config.DefaultBoolean(false)
    public static boolean ignoreNEIVersion;

    @Config.Comment("Font rendering replacements")
    @Config.DefaultBoolean(true)
    public static boolean enableFontRendererMixin;

    @Config.Comment("Larger values take more RAM, but require less model rebuilding (may reduce lag spikes). Unless you are very short on RAM, reducing this is not advised.")
    @Config.DefaultInt(1000)
    @Config.RangeInt(min = 1, max = 1_000_000)
    @Config.RequiresMcRestart
    public static int modelCacheSize;

    @Config.Comment("Enables various mixins that allow blocks to dynamically change their sound.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean blockSoundMixins;

    @Config.Comment("Enable item rendering modifications to allow rendering some items as translucent")
    @Config.DefaultBoolean(true)
    public static boolean enableTranslucentItemRenders;

    @Config.Comment("Enable client-side resource pack update checking for active packs")
    @Config.DefaultBoolean(true)
    public static boolean enableResourcePackUpdateCheck;

    @Config.Comment("Enable GTNH Team commands")
    @Config.DefaultBoolean(true)
    public static boolean enableTeamCommands;

    @Config.Comment("Root name for GTNH Team commands")
    @Config.DefaultString("gtnhteam")
    public static String teamCommandRoot;

    @Config.Comment("The public-facing name for the team system")
    @Config.DefaultString("GTNHTeams")
    public static String teamSystemName;

    @Config.Comment("If you're not a dev, you don't need this")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean enableTestBlocks;

    @Config.Comment("If you're not a dev, you don't need this")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean enableTestItems;

    @Config.Comment("Swaps out the block crack texture on modeled blocks, to make it easier to see the rotation.")
    @Config.DefaultBoolean(false)
    public static boolean testCrackTexture;

    @Config.Comment("Show the title/subtitle overlay on screen")
    @Config.DefaultBoolean(true)
    public static boolean enableTitleOverlay;

    @Config.Comment("Size of the main title text (default 4.0)")
    @Config.DefaultFloat(4.0f)
    @Config.RangeFloat(min = 0.5f, max = 8.0f)
    public static float titleScale;

    @Config.Comment("Size of the subtitle text (default 2.0)")
    @Config.DefaultFloat(2.0f)
    @Config.RangeFloat(min = 0.5f, max = 8.0f)
    public static float subtitleScale;

    @Config.Comment("Show item icons above the title when provided")
    @Config.DefaultBoolean(true)
    public static boolean showTitleIcon;

    @Config.Comment("Size of the icon above the title (default 2.0, so 32x32 pixels)")
    @Config.DefaultFloat(2.0f)
    @Config.RangeFloat(min = 0.5f, max = 8.0f)
    public static float titleIconScale;

    @Config.Comment("Vertical offset for the icon relative to the title text, in pixels. Negative = higher, positive = lower.")
    @Config.DefaultInt(0)
    @Config.RangeInt(min = -100, max = 100)
    public static int titleIconOffsetY;

    @Config.Comment("Icon animation style: none, fly_in, rise, slide, zoom, pop, spin, spin_reverse, bounce, wobble, swing, slam, tada")
    @Config.DefaultString("fly_in")
    public static String titleIconAnimation;

    @Config.Comment("Enable particle effects with title displays")
    @Config.DefaultBoolean(true)
    public static boolean enableTitleParticles;

    @Config.Comment("Cinematic completion effect tier: 0 = none, 3 = hyperspace, 4 = singularity, 5 = warp, 6 = lightspeed")
    @Config.DefaultInt(0)
    @Config.RangeInt(min = 0, max = 6)
    public static int titleEffectTier;

    @Config.Comment("Horizontal offset of the title block from screen center, in pixels. Negative = left, positive = right.")
    @Config.DefaultInt(0)
    @Config.RangeInt(min = -1000, max = 1000)
    public static int titleOffsetX;

    @Config.Comment("Vertical offset of the title block from screen center, in pixels. Negative = higher, positive = lower.")
    @Config.DefaultInt(0)
    @Config.RangeInt(min = -1000, max = 1000)
    public static int titleOffsetY;
}
