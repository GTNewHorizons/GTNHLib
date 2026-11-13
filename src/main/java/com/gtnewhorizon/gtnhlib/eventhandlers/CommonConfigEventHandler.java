package com.gtnewhorizon.gtnhlib.eventhandlers;

import com.gtnewhorizon.gtnhlib.templatedtext.TemplatedTextLoader;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class CommonConfigEventHandler {

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.PostConfigChangedEvent event) {
        // recache unit formatter since some units can be config dependent
        NumberFormatUtil.recacheUnitFormatters();
        // clear the memoization cache in case a mod config requires a config change.
        TemplatedTextLoader.clearCache();
    }
}
