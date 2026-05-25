package com.gtnewhorizon.gtnhlib.network;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.config.PacketSyncConfig;
import com.gtnewhorizon.gtnhlib.keybind.PacketKeyDown;
import com.gtnewhorizon.gtnhlib.network.base.NetworkChannel;
import com.gtnewhorizon.gtnhlib.network.teams.TeamDataSync;
import com.gtnewhorizon.gtnhlib.network.teams.TeamInfoSync;

public class NetworkHandler {

    public static final NetworkChannel instance = new NetworkChannel(GTNHLib.MODID);

    public static void init() {
        instance.toClient(new PacketMessageAboveHotbar());
        instance.toClient(new PacketSyncConfig());
        instance.toClient(new PacketViewDistance());
        instance.toClient(new MessageTitle());
        instance.toClient(new TeamInfoSync());
        instance.toClient(new TeamDataSync());

        instance.toServer(new PacketKeyDown());
    }

}
