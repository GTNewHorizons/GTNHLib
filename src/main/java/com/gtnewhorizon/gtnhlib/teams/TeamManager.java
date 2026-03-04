package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeamManager {

    protected static final List<Team> TEAMS = new ArrayList<>();
    protected static final Map<UUID, Set<Team>> PENDING_INVITES = new HashMap<>();
    // keyed by target team, value is the set of source teams requesting to merge into it
    protected static final Map<Team, Set<Team>> PENDING_MERGE_REQUESTS = new HashMap<>();

    public static boolean isTeamNameValid(String name) {
        if (name == null || name.isEmpty()) return false;
        for (Team team : TEAMS) {
            if (team.getTeamName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isInTeam(UUID playerUuid) {
        for (Team team : TEAMS) {
            if (team.isTeamMember(playerUuid)) {
                return true;
            }
        }
        return false;
    }

    public static Team getTeamByPlayer(UUID playerUuid) {
        for (Team team : TEAMS) {
            if (team.isTeamMember(playerUuid)) {
                return team;
            }
        }
        return null;
    }

    public static Team getTeamByName(String teamName) {
        for (Team team : TEAMS) {
            if (team.getTeamName().equals(teamName)) {
                return team;
            }
        }
        return null;
    }

    public static void addPendingMergeRequest(Team source, Team target) {
        Set<Team> mergeRequests = PENDING_MERGE_REQUESTS.get(target);
        if (mergeRequests == null) mergeRequests = new HashSet<>();
        mergeRequests.add(source);
        PENDING_MERGE_REQUESTS.put(target, mergeRequests);
    }

    public static Set<Team> getPendingMergeRequests(Team target) {
        return PENDING_MERGE_REQUESTS.get(target);
    }

    public static void removePendingMergeRequest(Team source, Team target) {
        Set<Team> mergeRequests = PENDING_MERGE_REQUESTS.get(target);
        if (mergeRequests == null) return;
        mergeRequests.remove(source);
        PENDING_MERGE_REQUESTS.put(target, mergeRequests);
    }

    public static boolean hasPendingMergeRequest(Team source, Team target) {
        Set<Team> mergeRequests = PENDING_MERGE_REQUESTS.get(target);
        if (mergeRequests == null) return false;
        return mergeRequests.contains(source);
    }

    public static void mergeTeams(Team surviving, Team consumed) {
        for (UUID uuid : consumed.getMembers()) surviving.addMember(uuid);

        for (String dataKey : TeamDataRegistry.getRegisteredKeys()) {
            surviving.getData(dataKey).mergeTeams(consumed.getData(dataKey));
        }
        TEAMS.remove(consumed);
        TeamWorldSavedData.markForSaving();
    }

    public static Team getOrCreateTeam(String playerName, UUID playerUuid) {
        for (Team team : TEAMS) {
            if (team.isTeamMember(playerUuid)) {
                return team;
            }
        }
        Team team = new Team(playerName + "'s Team");
        team.initializeData(TeamDataRegistry.getRegisteredKeys().toArray(new String[0]));
        team.addOwner(playerUuid);
        TEAMS.add(team);
        TeamWorldSavedData.markForSaving();
        return team;
    }

    public static void addPendingInvite(UUID uuid, Team team) {
        Set<Team> invites = PENDING_INVITES.get(uuid);
        if (invites == null) invites = new HashSet<>();
        invites.add(team);
        PENDING_INVITES.put(uuid, invites);
    }

    public static Set<Team> getPendingInvites(UUID uuid) {
        return PENDING_INVITES.get(uuid);
    }

    public static void removePendingInvite(UUID uuid, Team team) {
        Set<Team> invites = PENDING_INVITES.get(uuid);
        if (invites == null) return;
        invites.remove(team);
        if (invites.isEmpty()) PENDING_INVITES.remove(uuid);
    }

    public static void removeAllPendingInvites(UUID uuid) {
        PENDING_INVITES.remove(uuid);
    }

    /**
     * Adds a team, but only if the same team (same name, same owners and same members) does not already exist.
     *
     * @param team Team to be added.
     * @return True if a new team was added, false if this is a duplication of an existing team.
     *
     *         This fixes an issue where the same team would be saved twice to the NBT data after world reload in
     *         singleplayer.
     */
    public static boolean addTeamDeduplicated(Team team) {
        for (Team otherTeam : TEAMS) {
            if (team.getTeamName().equals(otherTeam.getTeamName()) && team.getOwners().equals(otherTeam.getOwners())
                    && team.getMembers().equals(otherTeam.getMembers())) {
                return false;
            }
        }
        TEAMS.add(team);
        return true;
    }

    public static void clear() {
        TEAMS.clear();
    }
}
