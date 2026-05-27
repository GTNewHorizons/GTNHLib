package com.gtnewhorizon.gtnhlib.eventhandlers;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatConfig;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ConfigEventHandler {

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(GTNHLib.MODID)) {
            // Reload number formatting settings immediately when config changes
            NumberFormatConfig.syncNumberFormatting();
        }
    }
}
