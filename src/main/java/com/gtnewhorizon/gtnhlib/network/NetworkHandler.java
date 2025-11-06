package com.gtnewhorizon.gtnhlib.network;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.config.PacketSyncConfig;
import com.gtnewhorizon.gtnhlib.keybind.PacketKeyDown;

import com.gtnewhorizon.gtnhlib.visualization.PacketVisualizedBox;
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
        instance.registerMessage(PacketKeyDown.HandlerKeyDown.class, PacketKeyDown.class, 2, Side.SERVER);
        instance.registerMessage(PacketViewDistance.Handler.class, PacketViewDistance.class, 3, Side.CLIENT);
        instance.registerMessage(PacketVisualizedBox.Handler.class, PacketVisualizedBox.class, 4, Side.CLIENT);
    }
}
