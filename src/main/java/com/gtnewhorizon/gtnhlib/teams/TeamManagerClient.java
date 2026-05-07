package com.gtnewhorizon.gtnhlib.teams;

import net.minecraft.nbt.NBTTagCompound;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.teams.TeamDataSync;
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

    public static void onTeamInfoSyncPacket(TeamInfoSync packet) {
        if (TEAM != null && TEAM.getTeamId().equals(packet.uuid)) {
            TEAM.renameTeam(packet.name);
        } else {
            TEAM = new Team(packet.name, packet.uuid, true);
            TEAM.initializeData(TeamDataRegistry.getRegisteredKeys().toArray(new String[0]));
        }
    }

    public static void onTeamDataSyncPacket(TeamDataSync packet) {
        if (TEAM != null) {
            for (Pair<String, NBTTagCompound> pair : packet.data) {
                ITeamData data = TEAM.getData(pair.getLeft());
                if (data instanceof INetworkTeamData networkTeamData) {
                    networkTeamData.fromPacketTag(pair.getRight(), packet.complete);
                } else {
                    GTNHLib.LOG.error("Invalid team data on client: {}", pair.getLeft());
                }
            }
        }
    }

    public static Team GetTeam() {
        return TEAM;
    }

}
