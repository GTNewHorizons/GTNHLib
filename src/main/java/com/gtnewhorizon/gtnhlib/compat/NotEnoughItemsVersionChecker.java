package com.gtnewhorizon.gtnhlib.compat;

import java.util.Locale;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.GTNHLibConfig;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;

public class NotEnoughItemsVersionChecker {

    @SubscribeEvent
    public void checkNEIVersion(ClientTickEvent event) {
        if (FMLClientHandler.instance().getClient().thePlayer == null) {
            return;
        }

        FMLCommonHandler.instance().bus().unregister(this);

        if (GTNHLibConfig.ignoreNEIVersion) {
            return;
        }

        ArtifactVersion neiVersion = Loader.instance().getIndexedModList().get("NotEnoughItems").getProcessedVersion();
        if (neiVersion.compareTo(new DefaultArtifactVersion("2.7.8-GTNH")) >= 0) {
            return; // NEI is at least version 2.7.8-GTNH
        }
        GTNHLib.proxy.addWarnToChat(
                String.format(
                        Locale.ROOT,
                        "Installed NEI version is to old to support RenderTooltipEvents! This may cause problems. Installed NEI version: %s (2.7.8-GTNH and newer support RenderTooltipEvents)",
                        neiVersion.getVersionString()));
    }
}
