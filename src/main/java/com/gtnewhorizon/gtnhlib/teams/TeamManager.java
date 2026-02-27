package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamManager {

    protected static final List<Team> TEAMS = new ArrayList<>();

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

    public static Team getTeam(String playerName, UUID playerUuid) {
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
