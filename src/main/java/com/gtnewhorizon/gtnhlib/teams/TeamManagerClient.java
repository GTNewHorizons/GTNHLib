package com.gtnewhorizon.gtnhlib.teams;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.teams.TeamInfoSync;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;

@EventBusSubscriber(side = Side.CLIENT)
public class TeamManagerClient {

    private static Team TEAM;

    @SubscribeEvent
    private static void onDisconnect(ClientDisconnectionFromServerEvent event) {
        TEAM = null;
    }

    public static void OnTeamInfoSyncPacket(TeamInfoSync packet) {
        if (TEAM != null && TEAM.getTeamId().equals(packet.uuid)) {
            TEAM.renameTeam(packet.name);
        } else {
            TEAM = new Team(packet.name, packet.uuid, true);
        }
        System.out.println("Team of " + TEAM.getTeamName() + " | " + TEAM.getTeamId());
    }

    public static Team GetTeam() {
        return TEAM;
    }

}
