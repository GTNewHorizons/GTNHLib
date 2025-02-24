package com.gtnewhorizon.gtnhlib.util;

import net.minecraft.client.Minecraft;

import com.gtnewhorizon.gtnhlib.ClientProxy;

@SuppressWarnings("unused")
public class ClientUtil {

    public static int getServerViewDistance() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isSingleplayer()) {
            return mc.gameSettings.renderDistanceChunks;
        } else {
            return ClientProxy.getCurrentServerViewDistance();
        }
    }
}
