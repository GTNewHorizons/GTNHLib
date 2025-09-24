package com.gtnewhorizon.gtnhlib;

import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = GTNHLib.MODID)
public class GTNHLibConfig {

    @Config.Comment("Set to true to no longer check if the NEI version is new enough to support RenderTooltipEvents")
    @Config.DefaultBoolean(false)
    public static boolean ignoreNEIVersion;

}
