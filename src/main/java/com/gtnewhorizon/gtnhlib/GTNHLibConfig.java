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

    @Config.Comment("Automatically load model textures based on model files. Disabling may cause models to lose their textures.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean autoTextureLoading;

    @Config.Comment("Ensures that blocks always return a valid icon for JSON model blocks, by overriding the block icon functions and passing the particle icon.")
    @Config.DefaultBoolean(true)
    public static boolean modelIconWrapperMixin;

    @Config.Comment("Enables various mixins that allow blocks to dynamically change their sound.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean blockSoundMixins;

    @Config.Comment("Enable item rendering modifications to allow rendering some items as translucent")
    @Config.DefaultBoolean(true)
    public static boolean enableTranslucentItemRenders;

    @Config.Comment("If you're not a dev, you don't need this")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean enableTestBlocks;

    @Config.Comment("If you're not a dev, you don't need this")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean enableTestItems;
}
