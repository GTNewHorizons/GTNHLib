package com.gtnewhorizon.gtnhlib;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = GTNHLib.MODID)
public class GTNHLibConfig {

    @Config.Comment("Set to true to no longer check if the NEI version is new enough to support RenderTooltipEvents")
    @Config.DefaultBoolean(false)
    public static boolean ignoreNEIVersion;

    @Config.Comment("Automatically load model textures based on model files. Disabling may cause models to lose their textures.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean autoTextureLoading;

    @Config.Comment("If you're not a dev, you don't need this")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean enableTestBlocks;

}
