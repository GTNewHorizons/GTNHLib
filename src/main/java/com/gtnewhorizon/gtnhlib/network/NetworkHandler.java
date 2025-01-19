package com.gtnewhorizon.gtnhlib.network;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.config.PacketSyncConfig;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class NetworkHandler {

    public static final SimpleNetworkWrapper instance = NetworkRegistry.INSTANCE.newSimpleChannel(GTNHLib.MODID);

    public static void init() {
        instance.registerMessage(
                PacketMessageAboveHotbar.HandlerMessageAboveHotbar.class,
                PacketMessageAboveHotbar.class,
                0,
                Side.CLIENT);
        instance.registerMessage(PacketSyncConfig.Handler.class, PacketSyncConfig.class, 1, Side.CLIENT);
    }
}
