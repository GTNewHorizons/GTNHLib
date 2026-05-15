package com.gtnewhorizon.gtnhlib.network.teams;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.gtnewhorizon.gtnhlib.teams.TeamRole;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class TeamInfoSync implements IMessage {

    public Map<UUID, String> teams;
    public Map<String, Pair<UUID, TeamRole>> playerTeamRole;

    public TeamInfoSync() {}

    public static TeamInfoSync createTeamInfoSync() {

        TeamInfoSync teamInfoSync = new TeamInfoSync();
        Map<UUID, Team> teamMap = TeamManager.getTeamMap();

        teamInfoSync.teams = new HashMap<>();
        teamMap.forEach((uuid, team) -> teamInfoSync.teams.put(uuid, team.getTeamName()));

        teamInfoSync.playerTeamRole = new HashMap<>();
        for (Team team : teamMap.values()) {
            Set<UUID> seen = new HashSet<>();

            for (UUID player : team.getOwners()) {
                teamInfoSync.playerTeamRole.put(
                        ServerPlayerUtils.getPlayerName(player),
                        new ImmutablePair<>(team.getTeamId(), TeamRole.OWNER));
                seen.add(player);
            }
            for (UUID player : team.getOfficers()) {
                if (seen.add(player)) {
                    teamInfoSync.playerTeamRole.put(
                            ServerPlayerUtils.getPlayerName(player),
                            new ImmutablePair<>(team.getTeamId(), TeamRole.OFFICER));
                }
            }
            for (UUID player : team.getMembers()) {
                if (seen.add(player)) {
                    teamInfoSync.playerTeamRole.put(
                            ServerPlayerUtils.getPlayerName(player),
                            new ImmutablePair<>(team.getTeamId(), TeamRole.MEMBER));
                }
            }
        }
        return teamInfoSync;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(teams.size());
        teams.forEach((key, value) -> {
            buf.writeLong(key.getMostSignificantBits());
            buf.writeLong(key.getLeastSignificantBits());
            ByteBufUtils.writeUTF8String(buf, value);
        });

        buf.writeInt(playerTeamRole.size());
        playerTeamRole.forEach((key, value) -> {
            ByteBufUtils.writeUTF8String(buf, key);
            buf.writeLong(value.getLeft().getMostSignificantBits());
            buf.writeLong(value.getLeft().getLeastSignificantBits());
            buf.writeShort((short) value.getRight().ordinal());
        });

    }

    @Override
    public void fromBytes(ByteBuf buf) {
        teams = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            teams.put(new UUID(buf.readLong(), buf.readLong()), ByteBufUtils.readUTF8String(buf));
        }

        playerTeamRole = new HashMap<>();
        size = buf.readInt();
        for (int i = 0; i < size; i++) {
            playerTeamRole.put(
                    ByteBufUtils.readUTF8String(buf),
                    new ImmutablePair<>(new UUID(buf.readLong(), buf.readLong()), TeamRole.values()[buf.readShort()]));
        }
    }
}
