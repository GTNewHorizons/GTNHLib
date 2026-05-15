package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.nbt.NBTTagCompound;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.teams.TeamDataSync;
import com.gtnewhorizon.gtnhlib.network.teams.TeamInfoSync;
import com.gtnewhorizon.gtnhlib.network.teams.TeamInviteSync;
import com.gtnewhorizon.gtnhlib.network.teams.TeamMergeSync;
import com.gtnewhorizon.gtnhlib.util.ClientPlayerUtils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;

@EventBusSubscriber(side = Side.CLIENT)
public class TeamManagerClient {

    private static Team TEAM;

    private static final Map<UUID, String> teams = new HashMap<>();
    private static final Map<String, Pair<UUID, TeamRole>> playerTeamRole = new HashMap<>();

    protected static final List<String> playersInvited = new ArrayList<>();
    protected static final List<UUID> teamsWhichInvitedPlayer = new ArrayList<>();
    protected static final List<UUID> teamsToBeMergedInto = new ArrayList<>();
    protected static final List<UUID> teamsRequestingConsume = new ArrayList<>();

    // Cached teamid -> players so UI doesn't have to recompute this every tick
    protected static Map<UUID, List<String>> teamOwners = new HashMap<>();
    protected static Map<UUID, List<String>> teamOfficers = new HashMap<>();
    protected static Map<UUID, List<String>> teamMembers = new HashMap<>();

    @SubscribeEvent
    private static void onDisconnect(ClientDisconnectionFromServerEvent event) {
        TEAM = null;
    }

    public static void onTeamInfoSyncPacket(TeamInfoSync packet) {
        teams.clear();
        playerTeamRole.clear();
        teamOwners.clear();
        teamOfficers.clear();
        teamMembers.clear();

        teams.putAll(packet.teams);
        playerTeamRole.putAll(packet.playerTeamRole); // test from here
        packet.playerTeamRole.forEach((key, value) -> {
            switch (value.getRight()) {
                case OWNER:
                    teamOwners.putIfAbsent(value.getLeft(), new ArrayList<>());
                    teamOwners.get(value.getLeft()).add(key);
                    break;
                case OFFICER:
                    teamOfficers.putIfAbsent(value.getLeft(), new ArrayList<>());
                    teamOfficers.get(value.getLeft()).add(key);
                    break;
                case MEMBER:
                    teamMembers.putIfAbsent(value.getLeft(), new ArrayList<>());
                    teamMembers.get(value.getLeft()).add(key);
            }
        });

        UUID playerTeamId = playerTeamRole.get(ClientPlayerUtils.getCurrentPlayerName()).getLeft();
        if (TEAM != null && playerTeamId.equals(TEAM.getTeamId())) {
            TEAM.renameTeam(teams.get(playerTeamId));
        } else {
            TEAM = new Team(teams.get(playerTeamId), playerTeamId, true);
            TEAM.initializeData(TeamDataRegistry.getRegisteredKeys().toArray(new String[0]));
        }

        teamOwners.values().forEach(value -> value.sort(String::compareTo));
        teamOfficers.values().forEach(value -> value.sort(String::compareTo));
        teamMembers.values().forEach(value -> value.sort(String::compareTo));
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

    @SuppressWarnings("unchecked")
    public static boolean isLastTeamOwner(String player) {
        return playerTeamRole.containsKey(player)
                && teamOwners.getOrDefault(playerTeamRole.get(player).getLeft(), Collections.EMPTY_LIST).size() <= 1;
    }

    public static boolean doesPlayerSatisfyTeamRole(String player, TeamRole role) {
        return playerTeamRole.containsKey(player) && playerTeamRole.get(player).getRight().ordinal() >= role.ordinal();
    }

    public static boolean playerIsOnTeam(String player, UUID team) {
        return playerTeamRole.containsKey(player) && playerTeamRole.get(player).getLeft().equals(team);
    }

    public static int getTeamSizeFromPlayer(String player) {
        if (!playerTeamRole.containsKey(player)) return 0;
        return getTeamSize(playerTeamRole.get(player).getLeft());
    }

    public static int getTeamSize(UUID teamId) {
        if (teamId == null) return 0;
        return teamOwners.getOrDefault(teamId, Collections.emptyList()).size()
                + teamOfficers.getOrDefault(teamId, Collections.emptyList()).size()
                + teamMembers.getOrDefault(teamId, Collections.emptyList()).size();
    }

    public static List<Pair<UUID, String>> getSortedTeamList() {
        return teams.entrySet().stream().map(e -> new ImmutablePair<>(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ImmutablePair::getRight)).collect(Collectors.toList());
    }

    public static TeamRole getRole(String player) {
        return playerTeamRole.getOrDefault(player, new ImmutablePair<>(null, null)).getRight();
    }

    public static String getName(UUID teamId) {
        return teams.get(teamId);
    }

    public static void onTeamPlayerInviteSyncPacket(TeamInviteSync message) {
        playersInvited.clear();
        teamsWhichInvitedPlayer.clear();

        playersInvited.addAll(message.outgoing);
        playersInvited.sort(String::compareTo);
        teamsWhichInvitedPlayer.addAll(message.incoming);
        teamsWhichInvitedPlayer.sort(Comparator.comparing(uuid -> teams.getOrDefault(uuid, "")));
    }

    public static void onTeamMergeSyncPacket(TeamMergeSync message) {
        teamsToBeMergedInto.clear();
        teamsRequestingConsume.clear();

        teamsToBeMergedInto.addAll(message.outgoing);
        teamsToBeMergedInto.sort(Comparator.comparing(uuid -> teams.getOrDefault(uuid, "")));
        teamsRequestingConsume.addAll(message.incoming);
        teamsRequestingConsume.sort(Comparator.comparing(uuid -> teams.getOrDefault(uuid, "")));
    }
}
