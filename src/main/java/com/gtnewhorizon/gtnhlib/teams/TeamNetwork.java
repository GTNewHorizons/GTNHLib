package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.teams.TeamDataSync;
import com.gtnewhorizon.gtnhlib.network.teams.TeamInfoSync;

public class TeamNetwork {

    protected static void sendPlayerAllTeamData(EntityPlayerMP player, Team team) {
        NetworkHandler.instance.sendTo(createTeamInfoSyncPacket(team), player);
        NetworkHandler.instance.sendTo(createCompleteTeamDataSyncPacket(team), player);
    }

    protected static TeamInfoSync createTeamInfoSyncPacket(Team team) {
        return new TeamInfoSync(team.getTeamId(), team.getTeamName());
    }

    protected static TeamDataSync createCompleteTeamDataSyncPacket(Team team) {
        List<Pair<String, NBTTagCompound>> list = new ArrayList<>();
        for (Entry<String, ITeamData> entry : team.getAllDataEntries()) {
            if (entry.getValue() instanceof INetworkTeamData networkTeamData) {
                NBTTagCompound tag = new NBTTagCompound();
                networkTeamData.toPacketTag(tag, true);
                list.add(Pair.of(entry.getKey(), tag));
            }
        }
        return new TeamDataSync(list);
    }

    /**
     * Sync {@link INetworkTeamData} with the client
     *
     * @param team The team to sync to
     * @param key  The key for the INetworkTeamData
     * @throws IllegalArgumentException If the team data for {@code key} is null or does not implement
     *                                  {@link INetworkTeamData}
     */
    public static void syncTeamData(Team team, String key) {
        ITeamData teamData = team.getData(key);
        if (teamData instanceof INetworkTeamData networkTeamData) {
            NBTTagCompound tag = new NBTTagCompound();
            networkTeamData.toPacketTag(tag, false);
            TeamDataSync packet = new TeamDataSync(key, tag);
            TeamManager.ForEachOnlineTeamMember(team, player -> NetworkHandler.instance.sendTo(packet, player));
            networkTeamData.markSyncedToClient();
            return;
        }
        throw new IllegalArgumentException("Unable to sync team data, " + key + " is not of type INetworkTeamData");
    }
}
